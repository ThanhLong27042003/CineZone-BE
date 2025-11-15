package com.longtapcode.identity_service.service;

import com.longtapcode.identity_service.constant.SeatInstanceStatus;
import com.longtapcode.identity_service.dto.request.SeatHoldListRequest;
import com.longtapcode.identity_service.dto.request.SeatHoldRequest;
import com.longtapcode.identity_service.dto.response.SeatPaymentResponse;
import com.longtapcode.identity_service.dto.response.SeatUpdateResponse;
import com.longtapcode.identity_service.dto.response.SeatUpdateSuccess;
import com.longtapcode.identity_service.entity.Booking;
import com.longtapcode.identity_service.entity.BookingDetail;
import com.longtapcode.identity_service.entity.Show;
import com.longtapcode.identity_service.entity.User;
import com.longtapcode.identity_service.exception.AppException;
import com.longtapcode.identity_service.exception.ErrorCode;
import com.longtapcode.identity_service.repository.BookingDetailRepository;
import com.longtapcode.identity_service.repository.BookingRepository;
import com.longtapcode.identity_service.repository.ShowRepository;
import com.longtapcode.identity_service.repository.UserRepository;
import lombok.AccessLevel;
import lombok.RequiredArgsConstructor;
import lombok.experimental.FieldDefaults;
import org.springframework.data.redis.core.StringRedisTemplate;
import org.springframework.data.redis.core.script.DefaultRedisScript;
import org.springframework.messaging.simp.SimpMessagingTemplate;
import org.springframework.stereotype.Service;

import java.util.*;
import java.util.concurrent.TimeUnit;

@Service
@RequiredArgsConstructor
@FieldDefaults(level = AccessLevel.PRIVATE, makeFinal = true)
public class SeatHoldService {

    StringRedisTemplate redisTemplate;
    SimpMessagingTemplate messagingTemplate;

    // ==================== LUA SCRIPT ĐỂ HOLD GHẾ ====================
    private final DefaultRedisScript<Long> holdSeatScript = new DefaultRedisScript<>(
            """
            local key = KEYS[1]
            local user = ARGV[1]
            local ttl = tonumber(ARGV[2])

            if redis.call("exists", key) == 0 then
                redis.call("set", key, user)
                redis.call("expire", key, ttl)
                return 1
            else
                return 0
            end
            """, Long.class
    );
    private final UserRepository userRepository;
    private final ShowRepository showRepository;
    private final BookingDetailRepository bookingDetailRepository;
    private final BookingRepository bookingRepository;

    public SeatUpdateSuccess holdSeat(SeatHoldRequest request) {
        Long showId = request.getShowId();
        String seatNumber = request.getSeatNumber();
        String userId = request.getUserId();

        if (isBooked(showId,seatNumber )) {
            return SeatUpdateSuccess.builder()
                    .success(false)
                    .message("Ghế đã được đặt!")
                    .build();
        }
        String key = "hold:" + showId + ":" + seatNumber;
        Long result = redisTemplate.execute(
                holdSeatScript,
                Collections.singletonList(key),
                userId, "120"
        );
        if(result != null){
            if(result == 1){
                Long ttl = getSeatTTL(showId, seatNumber);
                long expiresAt = System.currentTimeMillis() + (ttl != null ? ttl * 1000 : 0);

                SeatUpdateResponse message = SeatUpdateResponse.builder()
                        .showId(request.getShowId())
                        .userId(userId)
                        .seatNumber(request.getSeatNumber())
                        .status(SeatInstanceStatus.HELD.getStatus())
                        .expiresAt(expiresAt)
                        .build();

                messagingTemplate.convertAndSend(
                        "/topic/show/" + request.getShowId(),
                        message
                );

                return SeatUpdateSuccess.builder()
                        .success(true)
                        .message("Giữ ghế thành công!")
                        .build();
            }else{
                return SeatUpdateSuccess.builder()
                        .success(false)
                        .message("Ghế đã bị giữ!")
                        .build();
            }
        }
        return  SeatUpdateSuccess.builder()
                .success(false)
                .message("Lỗi giữ ghế")
                .build();
    }

    public boolean isBooked(Long showId, String seatNumber) {
        String key = "booked:" + showId + ":" + seatNumber;
        return Boolean.TRUE.equals(redisTemplate.hasKey(key));
    }

    public boolean isHeld(Long showId, String seatNumber) {
        String key = "hold:" + showId + ":" + seatNumber;
        return Boolean.TRUE.equals(redisTemplate.hasKey(key));
    }


    public SeatUpdateSuccess bookSeat(SeatHoldListRequest request) {
        Long showId = request.getShowId();
        Set<String> seatNumbers = request.getSeatNumbers();
        String userId = request.getUserId();

        User user = userRepository.findById(userId).orElseThrow(()-> new AppException(ErrorCode.USER_NOT_EXISTED));
        Show show = showRepository.findById(showId).orElseThrow(()-> new AppException(ErrorCode.SHOW_NOT_EXISTED));
        Booking booking = Booking.builder()
                .id1(user)
                .showID(show)
                .build();
         bookingRepository.save(booking);
        Set<BookingDetail> bookingDetails = new HashSet<>();

        for(String seatNumber : seatNumbers){
            String holdKey = "hold:" + showId + ":" + seatNumber;
            String bookedKey = "booked:" + showId + ":" + seatNumber;
            redisTemplate.opsForValue().set(bookedKey, userId);
            redisTemplate.delete(holdKey);

            BookingDetail bookingDetail = BookingDetail.builder()
                    .bookingID(booking)
                    .seatNumber(seatNumber)
                    .price(show.getPrice())
                    .build();

            bookingDetails.add(bookingDetail);
        }

        bookingDetailRepository.saveAll(bookingDetails);

        SeatPaymentResponse message = SeatPaymentResponse.builder()
                .showId(request.getShowId())
                .userId(userId)
                .seatNumbers(request.getSeatNumbers())
                .status(SeatInstanceStatus.BOOKED.getStatus())
                .expiresAt(0L)
                .build();

        messagingTemplate.convertAndSend(
                "/topic/show/" + request.getShowId(),
                message
        );

        return SeatUpdateSuccess.builder()
                .success(true)
                .message("Đặt ghế thành công!")
                .build();
    }

    public SeatUpdateSuccess releaseSeat(SeatHoldRequest request) {
        Long showId = request.getShowId();
        String seatNumber = request.getSeatNumber();
        String userId = request.getUserId();
        String key = "hold:" + showId + ":" + seatNumber;
        String currentHolder = redisTemplate.opsForValue().get(key);
        if (currentHolder != null && currentHolder.equals(userId)) {
            redisTemplate.delete(key);
            SeatUpdateResponse message = SeatUpdateResponse.builder()
                    .showId(request.getShowId())
                    .userId(userId)
                    .seatNumber(request.getSeatNumber())
                    .status(SeatInstanceStatus.AVAILABLE.getStatus())
                    .expiresAt(0L)
                    .build();

            messagingTemplate.convertAndSend(
                    "/topic/show/" + request.getShowId(),
                    message
            );
            return SeatUpdateSuccess.builder()
                    .success(true)
                    .message("Bỏ ghế thành công!")
                    .build();
        }
        return SeatUpdateSuccess.builder()
                .success(false)
                .message("Lỗi bỏ ghế")
                .build();
    }

    public List<SeatUpdateResponse> getOccupiedSeats(Long showId) {
        Set<String> holdKeys = redisTemplate.keys("hold:" + showId + ":*");
        Set<String> bookedKeys = redisTemplate.keys("booked:" + showId + ":*");
        List<SeatUpdateResponse> seatUpdateResponses = new ArrayList<>() ;
        if (holdKeys != null) {
            holdKeys.forEach(key->{
                Long ttl = redisTemplate.getExpire(key,TimeUnit.SECONDS);
                String userId = redisTemplate.opsForValue().get(key);
                seatUpdateResponses.add(SeatUpdateResponse.builder()
                        .showId(showId)
                        .userId(userId)
                        .seatNumber(key.split(":")[2])
                        .status(SeatInstanceStatus.HELD.getStatus())
                        .expiresAt(System.currentTimeMillis() + (ttl != null ? ttl * 1000 : 0 ))
                        .build());
            });
        }

        if (bookedKeys != null) {
            bookedKeys.forEach(key-> {
                String userId = redisTemplate.opsForValue().get(key);
                seatUpdateResponses.add(SeatUpdateResponse.builder()
                        .showId(showId)
                        .userId(userId)
                        .seatNumber(key.split(":")[2])
                        .status(SeatInstanceStatus.BOOKED.getStatus())
                        .expiresAt(0L)
                        .build());
            });

        }

        return seatUpdateResponses;
    }

    public Long getSeatTTL(Long showId, String seatNumber) {
        String key = "hold:" + showId + ":" + seatNumber;
        return redisTemplate.getExpire(key, TimeUnit.SECONDS);
    }
}
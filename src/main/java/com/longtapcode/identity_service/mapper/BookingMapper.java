package com.longtapcode.identity_service.mapper;

import com.longtapcode.identity_service.dto.response.BookingDetailResponse;
import com.longtapcode.identity_service.dto.response.BookingResponse;
import com.longtapcode.identity_service.entity.Booking;
import com.longtapcode.identity_service.entity.BookingDetail;
import org.mapstruct.Mapper;
import org.mapstruct.Mapping;

import java.util.Set;
import java.util.stream.Collectors;

@Mapper(componentModel = "spring")
public interface BookingMapper {

    @Mapping(target = "userId", source = "id1.id")
    @Mapping(target = "userName", source = "id1.userName")
    @Mapping(target = "userEmail", source = "id1.emailAddress")
    @Mapping(target = "showId", source = "showID.id")
    @Mapping(target = "movieTitle", source = "showID.movieID.title")
    @Mapping(target = "showDateTime", source = "showID.showDateTime")
    @Mapping(target = "bookingDetails", expression = "java(mapBookingDetails(booking))")
    BookingResponse toBookingResponse(Booking booking);

    @Mapping(target = "id", source = "id")
    @Mapping(target = "seatNumber", source = "seatNumber")
    @Mapping(target = "price", source = "price")
    BookingDetailResponse toBookingDetailResponse(BookingDetail bookingDetail);

    default Set<BookingDetailResponse> mapBookingDetails(Booking booking) {
        if (booking == null) return null;

        // Get booking details from repository
        return Set.of(); // This will be populated by service layer
    }
}
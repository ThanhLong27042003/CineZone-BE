package com.longtapcode.identity_service.service;

import com.longtapcode.identity_service.dto.response.CastResponse;
import com.longtapcode.identity_service.entity.Cast;
import com.longtapcode.identity_service.exception.AppException;
import com.longtapcode.identity_service.exception.ErrorCode;
import com.longtapcode.identity_service.mapper.CastMapper;
import com.longtapcode.identity_service.repository.CastRepository;
import lombok.AccessLevel;
import lombok.AllArgsConstructor;
import lombok.experimental.FieldDefaults;
import org.springframework.stereotype.Service;

import java.util.List;

@Service
@FieldDefaults(level = AccessLevel.PRIVATE, makeFinal = true)
@AllArgsConstructor
public class CastService {
    CastMapper castMapper;
    CastRepository castRepository;

    public List<CastResponse> getAllCast(){
        return castMapper.toListCastResponse(castRepository.findAll());
    }
    public List<CastResponse> searchCasts(String keyword){
        List<Cast> casts = castRepository.searchCasts(keyword);
        return castMapper.toListCastResponse(casts);
    }
    public CastResponse getCastById(Long castId){
        Cast cast = castRepository.findById(castId).orElseThrow(()->new AppException(ErrorCode.CAST_NOT_EXISTED));
        return castMapper.toCastResponse(cast);
    }
}

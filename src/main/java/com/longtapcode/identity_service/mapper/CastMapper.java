package com.longtapcode.identity_service.mapper;

import java.util.List;

import org.mapstruct.Mapper;

import com.longtapcode.identity_service.dto.response.CastResponse;
import com.longtapcode.identity_service.entity.Cast;

@Mapper(componentModel = "spring")
public interface CastMapper {
    List<CastResponse> toListCastResponse(List<Cast> cast);

    CastResponse toCastResponse(Cast cast);
}

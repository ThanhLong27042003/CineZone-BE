package com.longtapcode.identity_service.mapper;

import com.longtapcode.identity_service.dto.response.CastResponse;
import com.longtapcode.identity_service.entity.Cast;
import org.mapstruct.Mapper;

import java.util.List;

@Mapper(componentModel = "spring")
public interface CastMapper {
    List<CastResponse> toListCastResponse(List<Cast> cast);
    CastResponse toCastResponse(Cast cast);
}

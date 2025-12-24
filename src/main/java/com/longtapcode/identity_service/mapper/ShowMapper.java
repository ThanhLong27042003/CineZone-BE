package com.longtapcode.identity_service.mapper;

import com.longtapcode.identity_service.dto.request.ShowRequest;
import com.longtapcode.identity_service.dto.request.UpdateShowRequest;
import com.longtapcode.identity_service.dto.response.ShowResponse;
import com.longtapcode.identity_service.entity.Show;
import org.mapstruct.Mapper;
import org.mapstruct.Mapping;
import org.mapstruct.MappingTarget;
import java.util.List;


@Mapper(componentModel = "spring")
public interface ShowMapper {
    @Mapping(target = "movieID", ignore = true)
    @Mapping(target = "roomId", ignore = true)
    @Mapping(target = "showDateTime", expression = "java(java.time.LocalDateTime.of(request.getShowDate(), request.getShowTime()))")
    Show toShow(ShowRequest request);

    @Mapping(target = "showId", source="id")
    @Mapping(target="movieId", source="movieID.id")
    @Mapping(target = "roomId", source = "roomId.roomId")
    @Mapping(target = "roomName", source = "roomId.name")
    @Mapping(target="showDate", expression = "java(show.getShowDateTime().toLocalDate())")
    @Mapping(target="showTime", expression = "java(show.getShowDateTime().toLocalTime())")
    ShowResponse toShowResponse(Show show);

    List<ShowResponse> toListShowResponse(List<Show> shows);

    @Mapping(target = "movieID", ignore = true)
    @Mapping(target = "roomId", ignore = true)
    @Mapping(
            target = "showDateTime",
            expression = "java(java.time.LocalDateTime.of(request.getShowDate(), request.getShowTime()))"
    )
    void updateShow(@MappingTarget Show show, UpdateShowRequest request);
}

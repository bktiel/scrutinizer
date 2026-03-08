package com.example.medevac.pojos;

import com.example.medevac.entities.Assignment;
import com.example.medevac.entities.NineLineUser;

public record ResponderDto(
        Long id,
        String callsign
) {

    public static ResponderDto from(NineLineUser nineLineUser) {
        return new ResponderDto(nineLineUser.getId(), nineLineUser.getCallsign());
    }
}

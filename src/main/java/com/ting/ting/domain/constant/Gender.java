package com.ting.ting.domain.constant;

import lombok.Getter;
import lombok.RequiredArgsConstructor;

@RequiredArgsConstructor
@Getter
public enum Gender {
    MEN,
    WOMEN;

    public Gender getOpposite() {
        return this == MEN ? WOMEN : MEN;
    }
}

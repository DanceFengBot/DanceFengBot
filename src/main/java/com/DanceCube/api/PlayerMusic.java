package com.DanceCube.api;

import com.DanceCube.token.Token;
import com.Tools.HttpUtil;
import okhttp3.Response;

import java.util.Map;

public class PlayerMusic {
    public static Response gainMusicByCode(Token token, String code) {
        return HttpUtil.httpApi("https://dancedemo.shenghuayule.com/Dance/api/MusicData/GainMusicByCode?code=" + code,
                Map.of("Authorization", token.getBearerToken()),
                null);
    }
}
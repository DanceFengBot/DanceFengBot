package com.dancecube.api;

import com.dancecube.token.Token;
import com.tools.HttpUtil;
import net.mamoe.mirai.utils.ExternalResource;
import okhttp3.Response;
import com.mirai.config.AbstractConfig;

import java.io.File;
import java.util.Map;

public class PlayerMusic {
    public static Response gainMusicByCode(Token token, String code) {
        return HttpUtil.httpApi("https://dancedemo.shenghuayule.com/Dance/api/MusicData/GainMusicByCode?code=" + code,
                Map.of("Authorization", token.getBearerToken()),
                null);
    }
    public static ExternalResource getMusicCover(long num) {
        String coverPath = AbstractConfig.configPath + "/Images/Cover/OfficialImage/" + num + ".jpg";
        File coverFile = new File(coverPath);

        if (coverFile.exists() && coverFile.isFile()) {
            return (ExternalResource) coverFile;
        } else {
            return null;
        }
    }

}

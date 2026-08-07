package com.DanceCube.image;

import com.DanceCube.ratio.RankMusicInfo;
import com.google.gson.JsonObject;
import org.junit.Test;

import java.lang.reflect.Method;
import java.util.ArrayList;
import java.util.List;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertTrue;

public class LevelScoresImageTest {

    @Test
    public void totalPagesShouldBeCalculatedFromOfficialEntriesOnly() throws Exception {
        List<RankMusicInfo> musicInfos = new ArrayList<>();
        for (int i = 0; i < 150; i++) {
            musicInfos.add(createMusicInfo(i, 13, true));
        }
        for (int i = 150; i < 180; i++) {
            musicInfos.add(createMusicInfo(i, 13, false));
        }

        Method getTotalPages = LevelScoresImage.class.getDeclaredMethod("getTotalPages", List.class, int.class);
        getTotalPages.setAccessible(true);
        Method getPaginatedLevelScores = LevelScoresImage.class.getDeclaredMethod("getPaginatedLevelScores", List.class, int.class, int.class);
        getPaginatedLevelScores.setAccessible(true);

        int totalPages = (int) getTotalPages.invoke(null, musicInfos, 13);
        assertEquals(5, totalPages);

        @SuppressWarnings("unchecked")
        List<RankMusicInfo> page5 = (List<RankMusicInfo>) getPaginatedLevelScores.invoke(null, musicInfos, 13, 5);
        assertEquals(30, page5.size());
        assertFalse(page5.isEmpty());

        @SuppressWarnings("unchecked")
        List<RankMusicInfo> page6 = (List<RankMusicInfo>) getPaginatedLevelScores.invoke(null, musicInfos, 13, 6);
        assertTrue(page6.isEmpty());
    }

    private RankMusicInfo createMusicInfo(int id, int level, boolean official) {
        JsonObject details = new JsonObject();
        details.addProperty("MusicLevOld", level);
        details.addProperty("MusicRank", 0);
        details.addProperty("MusicLev", level);
        details.addProperty("PlayerPercent", 100000);
        details.addProperty("PlayerScore", 1000000);
        details.addProperty("ComboCount", 1000);
        details.addProperty("PlayerMiss", 0);
        details.addProperty("MusicRanking", 1);
        return new RankMusicInfo(id, "test" + id, official ? 1 : 0, details);
    }
}

package com.DanceCube.image;

import com.DanceCube.api.Ladder;
import com.DanceCube.api.LvRatioHistory;
import com.DanceCube.info.UserInfo;
import com.DanceCube.music.CoverUtil;
import com.DanceCube.ratio.AccGrade;
import com.DanceCube.ratio.RankMusicInfo;
import com.DanceCube.ratio.RatioCalculator;
import com.DanceCube.token.Token;
import com.Tools.image.ImageDrawer;
import com.Tools.image.ImageEffect;
import com.Tools.image.TextEffect;
import net.coobird.thumbnailator.Thumbnails;
import org.junit.Test;

import javax.imageio.ImageIO;
import java.awt.*;
import java.awt.image.BufferedImage;
import java.io.File;
import java.io.IOException;
import java.io.InputStream;
import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.ExecutionException;

import static com.DanceFengBot.config.AbstractConfig.configPath;
import static com.DanceFengBot.config.AbstractConfig.itIsAReeeeaaaalWindowsMark;

public class LevelScoresImage {
    public static final BufferedImage CARD_1; //低级
    public static final BufferedImage CARD_2; //中级
    public static final BufferedImage CARD_3; //高级

    public static final BufferedImage LV_SSS;
    public static final BufferedImage LV_SS;
    public static final BufferedImage LV_S;
    public static final BufferedImage LV_A;
    public static final BufferedImage LV_B;
    public static final BufferedImage LV_C;
    public static final BufferedImage LV_D;
    public static final BufferedImage AP;
    public static String path = configPath + "Images/UserRatioImage/";

    static {
        try {
            // 素材缓存到内存
            AP = ImageIO.read(new File(path + "AP.png"));
            CARD_1 = ImageIO.read(new File(path + "Card1.png"));
            CARD_2 = ImageIO.read(new File(path + "Card2.png"));
            CARD_3 = ImageIO.read(new File(path + "Card3.png"));
            LV_SSS = ImageIO.read(new File(path + "SSS.png"));
            LV_SS = ImageIO.read(new File(path + "SS.png"));
            LV_S = ImageIO.read(new File(path + "S.png"));
            LV_A = ImageIO.read(new File(path + "A.png"));
            LV_B = ImageIO.read(new File(path + "B.png"));
            LV_C = ImageIO.read(new File(path + "C.png"));
            LV_D = ImageIO.read(new File(path + "D.png"));
        } catch(IOException e) {
            throw new RuntimeException(e);
        }

    }
    public static InputStream generate(Token token,int level,int pages){
        UserInfo info;
        if(!itIsAReeeeaaaalWindowsMark()) {
            info = UserInfo.get(token);
        } else {
            CompletableFuture<UserInfo> userInfoFuture = CompletableFuture.supplyAsync(() -> UserInfo.get(token));
            try {
                info = userInfoFuture.get();
            } catch(ExecutionException | InterruptedException e) {
                info = UserInfo.get(token);
            }
        }
        ImageDrawer drawer;
        try {
            // 为什么这里放个finalInfo？
            if(info==null) {
                System.err.println("# 获取难度分数列表时个人信息获取失败");
            }
            Ladder ladder = Ladder.get(token).stream().filter(Ladder::getIsCurrent).findFirst().orElse(null);
            int rank = ladder != null && ladder.getIsCurrent() ? ladder.getLevelGrade() : -1;
            // 获取背景图片
            CompletableFuture<BufferedImage> backgroundImgFuture = CompletableFuture.supplyAsync(() -> {
                try {
                    // TODO 背景图片写到常量区（内存）
                    return ImageIO.read(new File(path + "Background4.png"));
                } catch(IOException e) {
                    throw new RuntimeException(e);
                }
            });

            //异步阻塞完绘制分数图
            drawer = new ImageDrawer(backgroundImgFuture.get());
            drawer.setAntiAliasing(); // 抗锯齿

            assert info != null;
            drawer.drawImage(ImageDrawer.read(info.getHeadimgURL()), 34, 180, 174, 174);
            drawer.drawImage(ImageDrawer.read(info.getHeadimgBoxPath()), -24, 122, 290, 290);
            if(!info.getTitleUrl().equals(""))
                drawer.drawImage(ImageDrawer.read(info.getTitleUrl()), 13, 373, 230, 79);
            if(rank == 0){
                drawer.drawImage(ImageDrawer.read("https://dancewebdemo.shenghuayule.com/dance/static/userCenter_img/quanminxingBadge0.png"), -60, 122, 183, 120);
            }else if(rank == 1) {
                drawer.drawImage(ImageDrawer.read("https://dancewebdemo.shenghuayule.com/dance/static/userCenter_img/quanminxingBadge1.png"), -60, 122, 183, 120);
            } else if(rank == 2){
                drawer.drawImage(ImageDrawer.read("https://dancewebdemo.shenghuayule.com/dance/static/userCenter_img/quanminxingBadge2.png"), -60, 122, 183, 120);
            } else if(rank == 3){
                drawer.drawImage(ImageDrawer.read("https://dancewebdemo.shenghuayule.com/dance/static/userCenter_img/quanminxingBadge3.png"), -60, 122, 183, 120);
            } else if(rank == 4){
                drawer.drawImage(ImageDrawer.read("https://dancewebdemo.shenghuayule.com/dance/static/userCenter_img/quanminxingBadge5.png"), -60, 122, 183, 120);
            } else if(rank == 5){
                drawer.drawImage(ImageDrawer.read("https://dancewebdemo.shenghuayule.com/dance/static/userCenter_img/quanminxingBadge6.png"), -60, 122, 183, 120);
            } else if(rank == 6){
                drawer.drawImage(ImageDrawer.read("https://dancewebdemo.shenghuayule.com/dance/static/userCenter_img/quanminxingBadge7.png"), -60, 122, 183, 120);
            }
        } catch(InterruptedException | ExecutionException e) {
            throw new RuntimeException(e);
        }

        int lvRatio = info.getLvRatio();
        String userInfoText = """
                %s
                
                战队：%s
                排名：%d
                战力：%d""".formatted(info.getUserName(), info.getTeamName(), info.getRankNation(), lvRatio);
        Font infoFont = new Font("得意黑", Font.PLAIN, 45);
        Font idFont = new Font("得意黑", Font.PLAIN, 30);

        drawer.color(Color.BLACK).font(idFont).drawText("ID："+token.getUserId(), 245, 200)
                .font(idFont).drawText("地区："+info.getCityName(), 245, 230)
                .font(infoFont).drawText(userInfoText, 245, 160, new TextEffect().setMaxWidth(230).setSpaceHeight(0));
        // 异步获取两个列表
        List<RankMusicInfo> allRankList;
        if(itIsAReeeeaaaalWindowsMark()) { // Windows下执行异步
            CompletableFuture<List<RankMusicInfo>> rankMusicFuture = CompletableFuture.supplyAsync(() -> RatioCalculator.getAllRankList(token.getBearerToken()));
            try {
                allRankList = rankMusicFuture.get();
            } catch(ExecutionException | InterruptedException e) {
                allRankList = RatioCalculator.getAllRankList(token.getBearerToken());
            }
        } else { //
            allRankList = RatioCalculator.getAllRankList(token.getBearerToken());
        }
        List<RankMusicInfo> levelScoresList = getPaginatedLevelScores(allRankList, level, pages);

        // AP30绘制
        int index = 0;
        int dx = 395, dy = 180; //x y延伸长度
        Font titleFont = new Font("Microsoft YaHei UI", Font.BOLD, 32);
        Font scoreFont = new Font("庞门正道标题体", Font.PLAIN, 52);
        Font comboMissAccFont = new Font("庞门正道标题体", Font.PLAIN, 15);
        Font levelFont = new Font("庞门正道标题体", Font.PLAIN, 23);

        out:
        for(int row = 0; row<10; row++) { //列
            for(int col = 0; col<3; col++, index++) { //行
                if(index>=levelScoresList.size()) break out;

                int dx2 = col * dx;
                int dy2 = row * dy;
                RankMusicInfo musicInfo = levelScoresList.get(index);
                BufferedImage cover = CoverUtil.getCoverOrDefault(musicInfo.getId());
                BufferedImage card = getCardImage(musicInfo.getDifficulty());
                BufferedImage grade = getGradeImage(musicInfo.getAccGrade());
                int fix = switch(musicInfo.getAccGrade()) {
                    case SSS, C -> 0;
                    case SS -> -17;
                    case S -> -6;
                    default -> 5;// case A B D
                };
                ImageEffect effect = new ImageEffect().setArc(35);

                // 战力 >xxxx(+/- xx)
                String diff = musicInfo.getRatioInt() > lvRatio
                        ? "+" + (musicInfo.getRatioInt() - lvRatio)
                        : String.valueOf(musicInfo.getRatioInt() - lvRatio);
                drawer.drawImage(cover, 16 + dx2, 621 + dy2, 130, 158, effect)
                        .drawImage(card, 15 + dx2, 620 + dy2)
                        .drawImage(grade, 285 + fix + dx2, 715 + dy2)
                        .font(titleFont, Color.BLACK)
                        .drawText(musicInfo.getName(), 160 + dx2, 624 + dy2, new TextEffect().setMaxWidth(220))
                        .font(scoreFont).drawText(String.valueOf(musicInfo.getScore()), 160 + dx2, 646 + dy2)
                        .font(comboMissAccFont)
                        .drawText("%d\n%d\n%.2f%%".formatted(musicInfo.getCombo(), musicInfo.getMiss(), musicInfo.getAccuracy()), 230 + dx2, 725 + dy2,
                                new TextEffect().setSpaceHeight(1))
                        .drawText("> %d (%s)".formatted(musicInfo.getRatioInt(), diff), 163 + dx2, 702 + dy2)
                        .font(levelFont, Color.WHITE)
                        .drawText(String.valueOf(musicInfo.getLevel()), 17 + dx2, 747 + dy2);
            }
        }

        float avg1 = RatioCalculator.average(levelScoresList);
        String extraInfoText = """
                %d分数列表
                战力：%.4f
                第%d页，共%d页
                """.formatted(level, avg1, pages, getTotalPages(allRankList, level));
        drawer.font(infoFont).color(Color.BLACK).drawText(extraInfoText, 720, 160, new TextEffect().setSpaceHeight(-6));
        drawer.dispose();
        return drawer.getImageStream("png");
    }

    private static int getTotalPages(List<RankMusicInfo> musicInfoList, int level) {
        return (int) Math.ceil((double) getLevelScoresForLevel(musicInfoList, level).size() / 30);
    }

    private static List<RankMusicInfo> getPaginatedLevelScores(List<RankMusicInfo> musicInfoList, int level, int page) {
        List<RankMusicInfo> validMusicList = getLevelScoresForLevel(musicInfoList, level);

        int startIndex = (page - 1) * 30;
        int endIndex = Math.min(page * 30, validMusicList.size());
        if (startIndex >= validMusicList.size()) {
            return new ArrayList<>(); // 如果起始索引超出范围，返回空列表
        }
        return validMusicList.subList(startIndex, endIndex);
    }

    private static List<RankMusicInfo> getLevelScoresForLevel(List<RankMusicInfo> musicInfoList, int level) {
        List<RankMusicInfo> validMusicList = new ArrayList<>();
        for (RankMusicInfo musicInfo : musicInfoList) {
            if (musicInfo.getLevel() == level && musicInfo.isOfficial()) {
                validMusicList.add(musicInfo);
            }
        }
        return validMusicList;
    }

    private static BufferedImage getGradeImage(AccGrade grade) {
        return switch(grade) {
            case SSS_AP -> AP;
            case SSS -> LV_SSS;
            case SS -> LV_SS;
            case S -> LV_S;
            case A -> LV_A;
            case B -> LV_B;
            case C -> LV_C;
            default -> LV_D;
        };
    }

    private static BufferedImage getCardImage(int difficulty) {
        return switch (difficulty) {
            case 0, -1 -> CARD_1; //-1为秀谱
            case 1 -> CARD_2;
            case 2 -> CARD_3;
            default -> CARD_1;
        };
    }
    @Test
    public void test() throws IOException {
        System.out.println("Running...");
        Token token = new Token(5559326,
                "I90WXSzVEoNyrk_Txagk9ZTFH2iH4VDR9OARoVQfLmw4c6MMCPyRWAQZNKq_Wj78jNfrfrh1x7JxVPC5jJN2zfGHvUC9Pl1B18r8OuBMx5uWZ9fL08jo1zvFaq9b7Sy7i8W9gp2Yx-Xv3QIoSGi5FvVAD7zmmEFdnhUBhUTv5PcMp8-x-sVzNwDPoe4JumaTxHWbMqBG8-XqH1QIJOvhF6e0vo3yyh_burunQh0Fr_Tf5PDbNZlBeFk_cUJKLwfRQ-YWmhW1f2dFbQ37w0YM3XMpYIY1AACr5zsiSXKbV3-Uin9pqNIjB9AKcY5ruaDwTORG4xEt9ydIkuJY3W-P0vjq_or6m5He1LTFuczWTOL5nuLGchJ047gBdNAgF3jX");
        String path = "C:\\Users\\Administrator\\IdeaProjects\\DFB\\result.png";

        InputStream image = generate(token, 13, 5);
        Thumbnails.of(image)
                .scale(1)
                .outputFormat("png")
                .toFile(path);
        System.out.println("Done!");
    }
}

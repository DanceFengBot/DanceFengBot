package com.DanceCube.image;


import com.DanceCube.info.AccountInfo;
import com.DanceCube.info.InfoStatus;
import com.DanceCube.info.ReplyItem;
import com.DanceCube.info.UserInfo;
import com.DanceCube.token.Token;
import com.Tools.image.ImageDrawer;
import com.Tools.image.TextEffect;
import org.junit.Test;
import com.DanceCube.api.Ladder;


import java.awt.*;
import java.io.InputStream;
import java.util.concurrent.ExecutionException;
import java.util.concurrent.Future;

import static com.DanceFengBot.command.AllCommands.scheduler;
import static com.DanceFengBot.config.AbstractConfig.configPath;
import static com.DanceFengBot.config.AbstractConfig.itIsAReeeeaaaalWindowsMark;


public class UserInfoImage {
    /**
     * 生成个人信息图，可能是用户查询别人账号
     *
     * @param token 发起查看信息的用户
     * @param id    需要查看的信息目标
     */
    public static InputStream generate(Token token, int id) {
        String bgPath = "file:" + configPath + "Images/UserInfoImage/Background2.png";

        UserInfo userInfo = UserInfo.get(token, id);
        Ladder ladder = Ladder.get(token).stream().filter(Ladder::isCurrent).findFirst().orElse(null);
        int rank = ladder != null && ladder.isCurrent() ? ladder.getLevelGrade() : -1;
        // 不存在查询的id
        if(userInfo.getStatus()==InfoStatus.NONEXISTENT) return null;

        //不存在用户
        if(userInfo.getHeadimgURL()==null) return null;

        ImageDrawer drawer = new ImageDrawer(bgPath);
        drawer.setAntiAliasing();
        drawer.drawImage(ImageDrawer.read(userInfo.getHeadimgURL()), 120, 150, 137, 137);

        if(!userInfo.getHeadimgBoxPath().equals("")) // 头像框校验
            drawer.drawImage(ImageDrawer.read(userInfo.getHeadimgBoxPath()), 74, 104, 230, 230);
        if(!userInfo.getTitleUrl().equals("")) // 头衔校验
            drawer.drawImage(ImageDrawer.read(userInfo.getTitleUrl()), 95, 300, 190, 68);
        if(rank == 0){
            drawer.drawImage(ImageDrawer.read("https://dancewebdemo.shenghuayule.com/dance/static/userCenter_img/quanminxingBadge0.png"), 25, 95, 183, 120);
        }else if(rank == 1) {
            drawer.drawImage(ImageDrawer.read("https://dancewebdemo.shenghuayule.com/dance/static/userCenter_img/quanminxingBadge1.png"), 25, 95, 183, 120);
        } else if(rank == 2){
            drawer.drawImage(ImageDrawer.read("https://dancewebdemo.shenghuayule.com/dance/static/userCenter_img/quanminxingBadge2.png"), 25, 95, 183, 120);
        } else if(rank == 3){
            drawer.drawImage(ImageDrawer.read("https://dancewebdemo.shenghuayule.com/dance/static/userCenter_img/quanminxingBadge3.png"), 25, 95, 183, 120);
        } else if(rank == 4){
            drawer.drawImage(ImageDrawer.read("https://dancewebdemo.shenghuayule.com/dance/static/userCenter_img/quanminxingBadge5.png"), 25, 95, 183, 120);
        } else if(rank == 5){
            drawer.drawImage(ImageDrawer.read("https://dancewebdemo.shenghuayule.com/dance/static/userCenter_img/quanminxingBadge6.png"), 25, 95, 183, 120);
        } else if(rank == 6){
            drawer.drawImage(ImageDrawer.read("https://dancewebdemo.shenghuayule.com/dance/static/userCenter_img/quanminxingBadge7.png"), 25, 95, 183, 120);
        }

        Font font = new Font("得意黑", Font.PLAIN, 36);
        Font font2 = new Font("得意黑", Font.PLAIN, 20);
        TextEffect effect = new TextEffect().setMaxWidth(235).setSpaceHeight(0);
        drawer.font(font);
        //信息开放
        if(userInfo.getStatus()!=InfoStatus.PRIVATE) {
            String gold = "不可见";
            String playedTimes = "不可见";
            String ladderScore = "不可见";
            if(token.getUserId()==id) {
                ReplyItem replyItem;
                AccountInfo accountInfo;
                // 异步获取个人信息
                if(itIsAReeeeaaaalWindowsMark()) {
                    accountInfo = AccountInfo.get(token);
                    replyItem = ReplyItem.get(token);
                } else {
                    try {
                        Future<ReplyItem> replyItemFuture = scheduler.async(() -> ReplyItem.get(token));
                        Future<AccountInfo> accountInfoFuture = scheduler.async(() -> AccountInfo.get(token));
                        Future<Ladder> ladderFuture = scheduler.async(() -> Ladder.get(token).stream().filter(Ladder::isCurrent).findFirst().orElse(null));
                        replyItem = replyItemFuture.get();
                        accountInfo = accountInfoFuture.get();
                        ladder = ladderFuture.get();
                    } catch(ExecutionException | InterruptedException e) {
                        accountInfo = AccountInfo.get(token);
                        replyItem = ReplyItem.get(token);
                    }
                }

                gold = String.valueOf(accountInfo.getGold());
                playedTimes = String.valueOf(replyItem.getPlayedTimes());
                ladderScore = String.valueOf(ladder.getLevelPoint());


            }
            drawer.drawText("%s\n\n战队：%s\n战力：%d\n金币：%s"
                            .formatted(userInfo.getUserName(),
                                    userInfo.getTeamName().equals("") ? "无" : userInfo.getTeamName(),
                                    userInfo.getLvRatio(),
                                    gold), 293, 137, effect)
                    .drawText("积分：%s\n全连率：%.2f%%\n全国排名：%d\n游玩次数：%s\n天梯分：%s"
                            .formatted(userInfo.getMusicScore(),
                                    (float) userInfo.getComboPercent() / 100,
                                    userInfo.getRankNation(),
                                    playedTimes, ladderScore), 106, 472, effect)
                    .font(font2)
                    .drawText("ID：" + userInfo.getUserID(), 293, 170);
        } else { //屏蔽
            drawer.drawText("%s\n\n地区：%s\n战力：%d"
                            .formatted(userInfo.getUserName(),
                                    userInfo.getCityName().equals("") ? "无" : userInfo.getCityName(),
                                    userInfo.getLvRatio()), 293, 137, effect)
                    .drawText("该账号已设置隐私", 106, 472)
                    .font(font2)
                    .drawText("ID：" + userInfo.getUserID(), 293, 170);

        }
        drawer.dispose();
        return drawer.getImageStream("PNG");
    }

    @Test
    public void test() {
        Token token = new Token(5559326, "pyBCTjsQXbcCJa2GpqA92HT7AUaixAuztdu7G61LvE7wsrB2gzS3yZ34z7wU5uBT-M5w2yf5_6NB_Ik7TpUv_kWezGUhfpxzTaHk8iT3wGpQQsdiUresZxe30piSuJe7meFEwHB0jDhxq07patSpK_WDCUDue3Sl4QKlVDl2hY-JQ7KP9xXqysoyUvi1Aj0iR1I9NyWQGl7fUWa8Ko9kOAlnGNqJGDXT2PX8s3qXPC88s0ZKN9bhIFaCk6-7Ivxtx6nemzdPN-TrPfr9M7Sbok2cgCiq-GJmUJ_AHqYQG3DAbAN19bbtjtXWjz5_D21DaHduGPCBF9WZRYBOdduT4f4WJSrBe6TNLAd10sSDWxiQ0nGAFXRFpovKpORjr6_Z");
        String path = "C:\\Users\\Administrator\\IdeaProjects\\DanceFengBot\\result.png";
        ImageDrawer.write(generate(token, 5559326), path);
    }
}


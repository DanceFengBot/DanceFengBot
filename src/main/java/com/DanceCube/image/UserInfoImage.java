package com.DanceCube.image;


import com.DanceCube.api.Ladder;
import com.DanceCube.info.AccountInfo;
import com.DanceCube.info.InfoStatus;
import com.DanceCube.info.ReplyItem;
import com.DanceCube.info.UserInfo;
import com.DanceCube.token.Token;
import com.Tools.image.ImageDrawer;
import com.Tools.image.TextEffect;
import org.junit.Test;

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
            drawer.drawImage(ImageDrawer.read(userInfo.getTitleUrl()), 108, 300, 161, 68);

        Font font = new Font("得意黑", Font.PLAIN, 36);
        Font font2 = new Font("得意黑", Font.PLAIN, 20);
        TextEffect effect = new TextEffect().setMaxWidth(235).setSpaceHeight(0);
        drawer.font(font);
        //信息开放
        if(userInfo.getStatus()!=InfoStatus.PRIVATE) {
            String gold = "不可见";
            String playedTimes = "不可见";
            String ladderScore = "不可见";
            int ladderRank = 0;
            if(token.getUserId()==id) {
                ReplyItem replyItem;
                AccountInfo accountInfo;
                Ladder ladder;
                // 异步获取个人信息
                if(itIsAReeeeaaaalWindowsMark()) {
                    accountInfo = AccountInfo.get(token);
                    replyItem = ReplyItem.get(token);
                    ladder = Ladder.get(token).isEmpty() ? null : Ladder.get(token).get(0);
                } else {
                    try {
                        Future<ReplyItem> replyItemFuture = scheduler.async(() -> ReplyItem.get(token));
                        Future<AccountInfo> accountInfoFuture = scheduler.async(() -> AccountInfo.get(token));
                        Future<Ladder> ladderFuture = scheduler.async(() -> Ladder.get(token).isEmpty() ? null : Ladder.get(token).get(0));
                        replyItem = replyItemFuture.get();
                        accountInfo = accountInfoFuture.get();
                        ladder = ladderFuture.get();
                    } catch(ExecutionException | InterruptedException e) {
                        accountInfo = AccountInfo.get(token);
                        replyItem = ReplyItem.get(token);
                        ladder = Ladder.get(token).isEmpty() ? null : Ladder.get(token).get(0);
                    }
                }

                gold = String.valueOf(accountInfo.getGold());
                playedTimes = String.valueOf(replyItem.getPlayedTimes());
                assert ladder != null;
                ladderScore = String.valueOf(ladder.getLevelPoint());
                ladderRank = ladder.getLevelGrade();
            }
            drawer.drawImage(ImageDrawer.read(rankIcon(ladderRank)), 125, 301, 137, 93);
            drawer.drawText("%s\n\n战队：%s\n战力：%d\n金币：%s"
                            .formatted(userInfo.getUserName(),
                                    userInfo.getTeamName().equals("") ? "无" : userInfo.getTeamName(),
                                    userInfo.getLvRatio(),
                                    gold), 293, 137, effect)
                    .drawText("积分：%s\n全连率：%.2f%%\n全国排名：%d\n游玩次数：%s\n天梯分：%s"
                            .formatted(userInfo.getMusicScore(),
                                    (float) userInfo.getComboPercent() / 100,
                                    userInfo.getRankNation(),
                                    playedTimes,ladderScore), 106, 472, effect)
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
    public static String rankIcon(int rank){
        if (rank == 0){
            return "http://dancewebdemo.shenghuayule.com/dance/static/userCenter_img/quanminxingBadge0.png";
        } else if (rank == 1) {
            return "http://dancewebdemo.shenghuayule.com/dance/static/userCenter_img/quanminxingBadge1.png";
        } else if (rank == 2) {
            return "http://dancewebdemo.shenghuayule.com/dance/static/userCenter_img/quanminxingBadge2.png";
        } else if (rank == 3) {
            return "http://dancewebdemo.shenghuayule.com/dance/static/userCenter_img/quanminxingBadge3.png";
        } else if (rank == 4) {
            return "http://dancewebdemo.shenghuayule.com/dance/static/userCenter_img/quanminxingBadge4.png";
        } else if (rank == 5) {
            return "http://dancewebdemo.shenghuayule.com/dance/static/userCenter_img/quanminxingBadge5.png";
        } else if (rank == 6) {
            return "http://dancewebdemo.shenghuayule.com/dance/static/userCenter_img/quanminxingBadge6.png";
        } else if (rank == 7) {
            return "http://dancewebdemo.shenghuayule.com/dance/static/userCenter_img/quanminxingBadge7.png";
        }
        return null;
    }
    @Test
    public void test() {
        Token token = new Token(5559326, "UlV1eztePPP2RXJZthbH6H2fTg94xm7tscLNAediVS3xgKJFRF6Sk-G9N4cz8bs4AvU-D-uuRTDSBKd-lxshMQViiwPA44xcn1IcKPTILNcBphqJjKOdcciIQtxtSS5sbhLRWVCl4-LdEHu9dcQ9a6Lq4liZpIf1sAeqdcq-PlSFV4xn6nPPl5M_dr_gKpM1xnQI1YfWTI_qE7HIa5JM9MIiHE3C33ZoRV3ls5htsq4AKjTdTpmoUgfdOhwDicuKz8D0r_7LZjVzxQw0OWyiWumiiXg6Bkfy6Ha7UUZAffgLO17vzRdPvCPnra0UNasOcljt8N8DnvvEjSfmz38lvMsqU4d14JfGR5y9qhcCRDUtJ372Jvyed0LgWhC0G-FJ");
        String path = "I:\\Bots\\DcConfig\\Images\\UserInfoImage\\result.png";
        ImageDrawer.write(generate(token, 5559326), path);
    }
}


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
        Ladder ladder = Ladder.get(token).stream()
                .filter(l -> l.isCurrent() && token.getUserId() == id) // 根据目标用户ID筛选
                .findFirst()
                .orElse(null);
        int rank = ladder != null ? ladder.getLevelGrade() : -1;
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
        ReplyItem replyItem = ReplyItem.get(token);
        //信息开放
        if(userInfo.getStatus()!=InfoStatus.PRIVATE) {
            String gold = "不可见";
            String playedTimes = "不可见";
            String ladderScore = "不可见";
            if(token.getUserId()==id) {
                AccountInfo accountInfo;
                // 异步获取个人信息
                if(itIsAReeeeaaaalWindowsMark()) {
                    accountInfo = AccountInfo.get(token);
                    replyItem = ReplyItem.get(token);
                } else {
                    try {
                        Future<ReplyItem> replyItemFuture = scheduler.async(() -> ReplyItem.get(token));
                        Future<AccountInfo> accountInfoFuture = scheduler.async(() -> AccountInfo.get(token));
//                        Future<Ladder> ladderFuture = scheduler.async(() -> Ladder.get(token).stream().filter(Ladder::isCurrent).findFirst().orElse(null));
                        replyItem = replyItemFuture.get();
                        accountInfo = accountInfoFuture.get();
//                        ladder = ladderFuture.get();
                    } catch(ExecutionException | InterruptedException e) {
                        accountInfo = AccountInfo.get(token);
                        replyItem = ReplyItem.get(token);
                    }
                }

                gold = String.valueOf(accountInfo.getGold());
                playedTimes = String.valueOf(replyItem.getPlayedTimes());
                ladderScore = String.valueOf(ladder.getLevelPoint());


            }
            drawer.drawText("%s\n\n战队：%s\n战力：%d\n金币：%s\n积分：%s"
                            .formatted(userInfo.getUserName(),
                                    userInfo.getTeamName().equals("") ? "无" : userInfo.getTeamName(),
                                    userInfo.getLvRatio(),
                                    gold,
                                    userInfo.getMusicScore()), 293, 115, effect)
                    .drawText("舞龄：%s\n全连率：%.2f%%\n全国排名：%d\n游玩次数：%s\n天梯分：%s"
                            .formatted(replyItem.getPlayedAge(),(float) userInfo.getComboPercent() / 100,
                                    userInfo.getRankNation(),
                                    playedTimes, ladderScore), 106, 472, effect)
                    .font(font2)
                    .drawText("ID：%d\n地区：%s"
                                    .formatted(userInfo.getUserID(),
                                            userInfo.getCityName().equals("") ? "无" : userInfo.getCityName()),
                            293, 156, effect);
        } else {
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
        Token token = new Token(5559326, "Qb3S0cgPFrVEF7PXZ4EkV-4YG5Coecb7KUrMjmRu_NqOlLld8-YlftapawAhe-gk2_keseytiSeIYAcoPXTVNWF14trFVP02D7agFhA2z_PEo5JLGfu6Mgi0f3XVxcoCn5li46c05S55PE0s7XV46HA2UTvtyRhMrggh00F4zbzRDbshp9mtS5kZxPjd2PXHsKJgbVgnQBCmnlBHXY9SYfpdXSH1Wb8qYb1PBfIKDP47K7ZfUmceVviXwJ9KQ6NHxg3KjPOs5vxe19RTWcGl5zmpMLCEy_2q5_gTvzdspPybVPgfIPJcHVwny7HZFAtWYRp7ovFB1ZAfCiLM0r-RYLA8-PbnLpdqrgtpI2XZq0uifsiJgnlRRRt5XeloQkYM");
        String path = "C:\\Users\\Administrator\\IdeaProjects\\DanceFengBot\\result.png";
        ImageDrawer.write(generate(token, 5559326), path);
    }
}


package com.DanceFengBot.event;

import com.DanceCube.token.Token;
import com.DanceCube.token.TokenBuilder;
import net.mamoe.mirai.Bot;
import net.mamoe.mirai.contact.Contact;
import net.mamoe.mirai.contact.Friend;
import net.mamoe.mirai.contact.friendgroup.FriendGroup;
import net.mamoe.mirai.event.EventHandler;
import net.mamoe.mirai.event.events.MessageEvent;
import net.mamoe.mirai.event.events.NewFriendRequestEvent;
import net.mamoe.mirai.event.events.NudgeEvent;
import net.mamoe.mirai.message.data.At;
import net.mamoe.mirai.message.data.MessageChain;
import net.mamoe.mirai.message.data.PlainText;

import static com.DanceFengBot.config.AbstractConfig.configPath;
import static com.DanceFengBot.config.AbstractConfig.userTokensMap;

// 不过滤通道
public class MainHandler {

    @EventHandler
    public static void eventCenter(MessageEvent event) {
        MessageChain messageChain = event.getMessage();
        if(messageChain.size() - 1==messageChain.stream()
                .filter(msg -> msg instanceof At | msg instanceof PlainText)
                .toList().size()) {
            PlainTextHandler.accept(event);
        } else return;

        String message = messageChain.contentToString();
        long qq = event.getSender().getId(); // qq发送者id 而非群聊id
        Contact contact = event.getSubject();

        // 文本消息检测
        switch(message) {
            case "#save" -> saveTokens(contact);
            case "#load" -> loadTokens(contact);
            case "#logout" -> logoutToken(contact);
        }
    }

    @EventHandler
    public static void NudgeHandler(NudgeEvent event) {
        if(event.getTarget() instanceof Bot) {
            event.getFrom().nudge().sendTo(event.getSubject());
        }
    }

    @EventHandler
    public static void addFriendHandler(NewFriendRequestEvent event) {
        event.accept();
        Friend friend = event.getBot().getFriend(event.getFromId());
        if(friend != null) {
            friend.sendMessage("🥰呐~ 现在我们是好朋友啦！\n请发送“help”查看功能哦！");
            FriendGroup friendGroup = event.getBot().getFriendGroups().get(0);
            if(friendGroup != null) {
                friendGroup.moveIn(friend);
            }
        }
    }


    /**
     * 保存Token到文件JSON
     *
     * @param contact 触发对象
     */
    public static void saveTokens(Contact contact) {
        TokenBuilder.tokensToFile(userTokensMap, configPath + "UserTokens.json");
        contact.sendMessage("保存成功！共%d条".formatted(userTokensMap.size()));
    }

    /**
     * 从文件JSON中加载Token
     *
     * @param contact 触发对象
     */
    public static void loadTokens(Contact contact) {
        String path = configPath + "UserTokens.json";
        userTokensMap = TokenBuilder.tokensFromFile(path, false);
        contact.sendMessage("不刷新加载成功！共%d条".formatted(userTokensMap.size()));
    }

    /**
     * 注销Token
     *
     * @param contact 触发对象
     */
    public static void logoutToken(Contact contact) {
        long qq = contact.getId();
        Token token = userTokensMap.get(qq);
        if(token == null) {
            contact.sendMessage("当前账号未登录到舞小铃！");
            return;
        }
        userTokensMap.remove(qq);
        contact.sendMessage("id:%d 注销成功！".formatted(token.getUserId()));
    }

}

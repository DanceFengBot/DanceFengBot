package com.DanceFengBot;

import com.DanceCube.token.Token;
import com.DanceCube.token.TokenBuilder;
import com.DanceFengBot.event.MainHandler;
import com.DanceFengBot.init.DatabaseInit;
import com.DanceFengBot.init.InsertAudioDataInit;
import com.DanceFengBot.init.InsertCoverDataInit;
import com.DanceFengBot.init.CoverDownloadInit;
import com.DanceFengBot.task.SchedulerTask;
import com.Tools.DatabaseConnection;
import net.mamoe.mirai.console.extension.PluginComponentStorage;
import net.mamoe.mirai.console.plugin.jvm.JavaPlugin;
import net.mamoe.mirai.console.plugin.jvm.JavaPluginScheduler;
import net.mamoe.mirai.console.plugin.jvm.JvmPluginDescriptionBuilder;
import net.mamoe.mirai.event.Event;
import net.mamoe.mirai.event.EventChannel;
import net.mamoe.mirai.event.GlobalEventChannel;
import net.mamoe.mirai.event.events.MessageEvent;
import net.mamoe.mirai.event.events.NewFriendRequestEvent;
import net.mamoe.mirai.event.events.NudgeEvent;
import org.jetbrains.annotations.NotNull;

import java.io.IOException;
import java.sql.SQLException;
import java.text.SimpleDateFormat;
import java.util.*;
import java.util.logging.Logger;

import static com.DanceFengBot.config.AbstractConfig.configPath;
import static com.DanceFengBot.config.AbstractConfig.userTokensMap;

public final class DanceFengBot extends JavaPlugin {
    public static final DanceFengBot INSTANCE = new DanceFengBot();
    String path = configPath + "UserTokens.json";

    private DanceFengBot() {
        super(new JvmPluginDescriptionBuilder( // 必要属性
                        "com.DanceFengBot.DanceFengBot", // id
                        "1.3" // version
                ).author("Lin,Jingsong2008") // 可选属性，可以不提供， 直接 build
                        .build()
        );
    }

    @Override
    public void onLoad(@NotNull PluginComponentStorage $this$onLoad) {
        super.onLoad($this$onLoad);
    }

    @Override
    public void onEnable() {
        getLogger().info("Plugin loaded!");

        // 连接数据库（在加载Token之前先连接数据库）
        try {
            if (!DatabaseConnection.connect()) {
                getLogger().error("数据库连接失败，插件将关闭！");
                onDisable(); // 关闭插件
                return; // 提前返回，不再执行后续初始化
            }
            getLogger().info("数据库连接成功！");
        } catch (Exception e) {
            getLogger().error("数据库连接异常: " + e.getMessage());
            onDisable();
            return;
        }
        EventChannel<Event> channel = GlobalEventChannel.INSTANCE
                .parentScope(DanceFengBot.INSTANCE)
                .context(this.getCoroutineContext());
        // 输出加载Token
        onLoadToken();
        // 初始化
        if (DatabaseInit.checkInitMark()){
            getLogger().info("数据库已初始化，无需重复初始化");
            return;
        } else {
            DatabaseInit.init();
            InsertCoverDataInit.init();
            InsertAudioDataInit.init();
            try {
                CoverDownloadInit.Downloader();
            } catch (IOException e) {
                throw new RuntimeException(e);
            }
            DatabaseInit.createInitMark();
        }
        // Token刷新器
        SchedulerTask.autoRefreshToken();
        // 每月15日新歌
        SchedulerTask.addNewMusicCover();
        SchedulerTask.addNewMusicAudio();
        SchedulerTask.downloadNewMusicCover();
        // 监听器
        channel.subscribeAlways(MessageEvent.class, event -> {
            try {
                MainHandler.eventCenter(event);
            } catch (IOException | SQLException e) {
                throw new RuntimeException(e);
            }
        });
        channel.subscribeAlways(NudgeEvent.class, MainHandler::NudgeHandler);
        channel.subscribeAlways(NewFriendRequestEvent.class, MainHandler::addFriendHandler);
    }

    @Override
    public void onDisable() {
        // 保存Tokens
        TokenBuilder.tokensToFile(userTokensMap, configPath + "UserTokens.json");
        System.out.printf("保存成功！共%d条%n", userTokensMap.size());

        // 断开数据库连接
        DatabaseConnection.disconnect();
        getLogger().info("数据库连接已关闭");
    }

    @Deprecated
    public void refreshTokensTimer() {
        long period = 86400 * 500; //半天

        JavaPluginScheduler scheduler = DanceFengBot.INSTANCE.getScheduler();
        userTokensMap = TokenBuilder.tokensFromFile(path);

        TimerTask task = new TimerTask() {
            @Override
            public void run() {
                Token defaultToken = userTokensMap.get(0L);
                if(defaultToken==null) {
                    userTokensMap.forEach((qq, token) ->
                            scheduler.async(() -> {
                                if(token.checkAvailable()) token.refresh();
                            }));
                } else {
                    userTokensMap.forEach((qq, token) ->
                            scheduler.async(() -> {
                                // 默认token不为用户token
                                if(token.checkAvailable() &
                                        !defaultToken.getAccessToken().equals(token.getAccessToken()))
                                    token.refresh();
                            }));

                }
                TokenBuilder.tokensToFile(userTokensMap, path);
                System.out.println(new SimpleDateFormat("MM-dd hh:mm:ss").format(new Date()) + ": 今日已刷新token");
            }
        };

        new Timer().schedule(task, 0, 86400);
    }

    public void onLoadToken() {
        StringBuilder sb = new StringBuilder();
        // 导入Token
        userTokensMap = Objects.requireNonNullElse(
                TokenBuilder.tokensFromFile(configPath + "UserTokens.json"),
                new HashMap<>());

        for(Map.Entry<Long, Token> entry : userTokensMap.entrySet()) {
            Long qq = entry.getKey();
            Token token = entry.getValue();
            sb.append("\nqq: %d , id: %s;".formatted(qq, token.getUserId()));
        }
        Logger.getGlobal().info(("刷新加载成功！共%d条".formatted(userTokensMap.size()) + sb));
    }
    public void consoleCommands(){

    }
}
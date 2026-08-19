package com.DanceFengBot.command;

import com.DanceCube.api.Machine;
import com.DanceCube.api.PhoneLoginBuilder;
import com.DanceCube.api.PlayerMusic;
import com.DanceCube.image.*;
import com.DanceCube.info.ReplyItem;
import com.DanceCube.ratio.RatioCalculator;
import com.DanceCube.ratio.RecentMusicInfo;
import com.DanceCube.token.Token;
import com.DanceCube.token.TokenBuilder;
import com.DanceFengBot.DanceFengBot;
import com.DanceFengBot.config.UserConfigUtils;
import com.Tools.HttpUtil;
import kotlin.jvm.functions.Function1;
import net.coobird.thumbnailator.Thumbnails;
import net.mamoe.mirai.console.plugin.jvm.JavaPluginScheduler;
import net.mamoe.mirai.contact.Contact;
import net.mamoe.mirai.contact.Group;
import net.mamoe.mirai.event.Event;
import net.mamoe.mirai.event.EventChannel;
import net.mamoe.mirai.event.GlobalEventChannel;
import net.mamoe.mirai.event.events.MessageEvent;
import net.mamoe.mirai.message.data.*;
import okhttp3.Response;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

import java.awt.image.BufferedImage;
import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.lang.reflect.Field;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Objects;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.ExecutionException;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.TimeoutException;
import java.util.function.BiConsumer;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

import static com.DanceFengBot.config.AbstractConfig.*;

@SuppressWarnings("unused")
public class AllCommands {


    public static JavaPluginScheduler scheduler = DanceFengBot.INSTANCE.getScheduler();
    public static HashSet<RegexCommand> regexCommands = new HashSet<>();  //所有正则指令
    public static HashSet<ArgsCommand> argsCommands = new HashSet<>();  //所有参数指令
    private static final BiConsumer<Contact, Long> onNoLoginCall = (contact, qq) ->
            contact.sendMessage("好像还没有登录诶(´。＿。｀)\n私信发送\"登录\"一起来玩吧！");
    private static final BiConsumer<Contact, Long> onInvalidCall = (contact, qq) ->
            contact.sendMessage("小枫看到登录身份过期了💦\n重新私信登录恢复吧💦");

    @Deprecated
    public static Token defaultToken = Objects.requireNonNullElse(userTokensMap.get(0L), new Token(""));

    // 初始化所有指令
    public static void init() {
        for(Field field : AllCommands.class.getDeclaredFields()) {
            field.setAccessible(true);
            if(field.isAnnotationPresent(DeclaredCommand.class)) {
                try {
                    if(field.getType() == RegexCommand.class)
                        regexCommands.add((RegexCommand) field.get(null)); // 获取并保存所有指令
                    else if(field.getType() == ArgsCommand.class) {
                        argsCommands.add(((ArgsCommand) field.get(null)));
                    }
                } catch(IllegalAccessException e) {
                    throw new RuntimeException(e);
                }
            }
        }
    }

    @DeclaredCommand("help")
    public static final RegexCommand msgMenu = new RegexCommandBuilder()
            .multiStrings("菜单", "help", "帮助")
            .onCall(Scope.GLOBAL, (event, contact, qq, args) -> {
                contact.sendMessage("详见https://www.kdocs.cn/l/ccLRaUCuNMX0。");
            }).build();

    @DeclaredCommand("舞立方机器人登录")
    public static final RegexCommand dcLogin = new RegexCommandBuilder()
            .multiStrings("登录", "舞立方登录")
            .onCall(Scope.GLOBAL, (event, contact, qq, args) -> {
                // 限私聊
                if(contact instanceof Group) {
                    contact.sendMessage("私信才可以登录哦( •̀ ω •́ )/");
                    return;
                }
                // 正在登录检测
                if(logStatus.contains(qq)) {
                    contact.sendMessage("(´。＿。｀)不要重复登录啊喂！");
                    return;
                }
                TokenBuilder builder = new TokenBuilder();
                Image image = HttpUtil.getImageFromURL(builder.getQrcodeUrl(), contact);

                contact.sendMessage(new PlainText("🤗快用微信扫码，在五分钟内登录上吧~").plus(image));

                logStatus.add(qq);
                Token token = builder.getToken();

                if(token == null) {
                    contact.sendMessage("超时啦~ 请重试一下吧！");
                } else {
                    contact.sendMessage("登录成功啦~(●'◡'●)\n你的ID是：%s\n\n⭐要是账号不匹配的话，重新发送登录就好了".formatted(token.getUserId()));
                    userTokensMap.put(qq, token);  // 重复登录只会覆盖新的token
                    contact.sendMessage("欢迎加入机器人的QQ群聊：908659951。");
                }
                logStatus.remove(qq);
            }).build();


    @DeclaredCommand("舞立方机器人退出登录")
    public static final RegexCommand dcLogout = new RegexCommandBuilder()
            .regex("退出登录")
            .onCall(Scope.GLOBAL, (event, contact, qq, args) -> {
                // 限私聊
                if(contact instanceof Group) {
                    contact.sendMessage("私信才可以使用哦( •̀ ω •́ )/");
                    return;
                }
                // 正在登录检测
                if(logStatus.contains(qq)) {
                    contact.sendMessage("(´。＿。｀)你正在登录诶！登录后再试试吧");
                    return;
                }
                // 未登录检测
                if(!userTokensMap.containsKey(qq)) {
                    contact.sendMessage("舞小枫这没有过你的账号！");
                } else {
                    userTokensMap.remove(qq);
                    contact.sendMessage("退出登录成功！");
                }
            }).build();



    @Deprecated
    @DeclaredCommand("舞立方机台登录")
    public static final ArgsCommand machineLogin = new ArgsCommandBuilder()
            //Todo：扫不出来
//            .multiStrings("机台登录")
            .prefix("机台登录", "jt")
            .form(ArgsCommand.CHAR)
            .onCall(Scope.GLOBAL, (event, contact, qq, args) -> {
                if(args == null) {
                    contact.sendMessage("请在QQ扫码后复制链接\n格式：机台登录/jt (链接)");
                }

                Token token = getToken(contact, qq, onNoLoginCall, onInvalidCall);
                if(token == null) return;

                if(args != null) {
                    String link = args[0];
                    try(Response response = Machine.qrLogin(token, link)) {
                        if(response != null && response.code() == 200) {
                            contact.sendMessage(new QuoteReply(event.getMessage()).plus("登录成功辣，快来出勤吧！"));
                        } else {
                            contact.sendMessage(new QuoteReply(event.getMessage()).plus("链接失效了，换一个试试看吧"));
                        }
                    }//401 404

                }

//
//                MessageChain messageChain = event.getMessage();
//                EventChannel<Event> channel = GlobalEventChannel.INSTANCE.parentScope(MiraiBot.INSTANCE)
//                        .filter(ev -> ev instanceof MessageEvent && ((MessageEvent) ev).getSender().getId()==qq);
//                CompletableFuture<MessageEvent> future = new CompletableFuture<>();
//                channel.subscribeOnce(MessageEvent.class, future::complete);
//
//                contact.sendMessage(new QuoteReply(messageChain).plus(new PlainText("请在3分钟之内发送机台二维码图片！\n一定要清楚才好！")));
//                SingleMessage message;
//                try {
//                    MessageChain nextMessage = future.get(3, TimeUnit.MINUTES).getMessage();
//                    List<SingleMessage> messageList = nextMessage.stream().filter(m -> m instanceof PlainText).toList();
//                    if(messageList.size()!=1) {
//                        contact.sendMessage(new QuoteReply(nextMessage).plus(new PlainText("这个不是图片吧...重新发送“机台登录”吧")));
//                    } else {  // 第一个信息
//                        message = messageList.get(0);
//                        String imageUrl = Image.queryUrl((Image) message);
//                        String qrUrl = HttpUtil.qrDecodeTencent(imageUrl);
//                        if(qrUrl==null) {  // 若扫码失败
//                            contact.sendMessage(new QuoteReply((MessageChain) message).plus(new PlainText("没有扫出来！再试一次吧！")));
//                            return;
//                        }
//                        try(Response response = Machine.qrLogin(token, qrUrl)) {
//                            if(response!=null && response.code()==200) {
//                                contact.sendMessage("登录成功辣，快来出勤吧！");
//                            } else {
//                                contact.sendMessage("二维码失效了，换一个试试看吧");
//                            }
//                        }//401 404
//                    }
//                } catch(InterruptedException | ExecutionException e) {
//                    e.printStackTrace();
//                } catch(TimeoutException e) {
//                    e.printStackTrace();
//                    contact.sendMessage(new QuoteReply(messageChain).plus("超时啦，请重新发送吧~"));
//                }
            }).build();

    @DeclaredCommand("借号扫码登录")
    public static final ArgsCommand borrowMachineLogin = new ArgsCommandBuilder()
            .prefix("借号")
            .form(ArgsCommand.NUMBER)
            .onCall(Scope.USER, (event, contact, qq, args) -> {
                long friend = 0;
                if(args != null) friend = Long.parseLong(args[0]);

                Token token = getToken(contact, friend,
                        ((c, l) -> contact.sendMessage("对方没有登录！这个账号借不到了诶...")),
                        (c, l) -> contact.sendMessage("过期！这个账号借不到了诶..."));
                if(token == null) {
                    return;
                } else if(token.getUserId() != 939088) {
                    contact.sendMessage("未开放其它账号，不许登录！");
                }

                MessageChain messageChain = event.getMessage();
                EventChannel<Event> channel = GlobalEventChannel.INSTANCE.parentScope(DanceFengBot.INSTANCE);//.filter(getContactFilter(event));
                CompletableFuture<MessageEvent> future = new CompletableFuture<>();
                channel.subscribeOnce(MessageEvent.class, future::complete);

                contact.sendMessage(new QuoteReply(messageChain).plus(new PlainText("请在3分钟之内发送机台二维码图片哦！\n一定要清楚才好！")));
                SingleMessage message;
                try {
                    MessageChain nextMessage = future.get(3, TimeUnit.MINUTES).getMessage();
                    List<SingleMessage> messageList = nextMessage.stream().filter(m -> m instanceof Image).toList();
                    if(messageList.size() != 1) {
                        contact.sendMessage(new QuoteReply(nextMessage).plus(new PlainText("这个不是图片吧...重新发送“机台登录”吧")));
                    } else {  // 第一个信息
                        message = messageList.get(0);
                        String imageUrl = Image.queryUrl((Image) message);
                        String qrUrl = HttpUtil.qrDecodeTencent(imageUrl);
                        if(qrUrl == null) {  // 若扫码失败
                            contact.sendMessage(new QuoteReply((MessageChain) message).plus(new PlainText("没有扫出来！再试一次吧！")));
                            return;
                        }
                        try(Response response = Machine.qrLogin(token, qrUrl)) {
                            if(response != null && response.code() == 200) {
                                contact.sendMessage("借号成功辣，快来出勤吧！");
                            } else {
                                contact.sendMessage("二维码失效了，换一个试试看吧");
                            }
                        }//401 404
                    }
                } catch(InterruptedException | ExecutionException e) {
                    e.printStackTrace();
                } catch(TimeoutException e) {
                    e.printStackTrace();
                    contact.sendMessage(new QuoteReply(messageChain).plus("超时啦，请重新发送吧~"));
                }
            })
            .onCall(Scope.GROUP, (event, contact, qq, args) ->
                    contact.sendMessage("私聊才能借号！"))
            .build();

    @NotNull
    private static Function1<? super Event, Boolean> getContactFilter(MessageEvent event) {
        return it -> {
            if(!(it instanceof MessageEvent another)) return false;
            // 过滤出发送者
            if(event.getSubject().getId() != another.getSubject().getId()) return false;
            return event.getSender().getId() == another.getSender().getId();
        };
    }

    @DeclaredCommand("个人信息")
    public static final RegexCommand msgUserInfo = new RegexCommandBuilder()
            .multiStrings("个人信息", "看看我的", "我的信息", "我的舞立方", "mydc", "mywlf")
            .onCall(Scope.GLOBAL, (event, contact, qq, args) -> {
                Token token = getToken(contact, qq, onNoLoginCall, onInvalidCall);
                if(token == null) {
                    return;
                }

                InputStream inputStream = UserInfoImage.generate(token, token.getUserId());
                if(inputStream != null) {
                    Image image = HttpUtil.getImageFromStream(inputStream, contact);
                    contact.sendMessage(image);
                }
            }).build();

    @DeclaredCommand("舞立方自制谱兑换")
    public static final RegexCommand gainMusicByCode = new RegexCommandBuilder()
            .regex("[a-zA-Z0-9]{15}", false)
            .onCall(Scope.USER, (event, contact, qq, args) -> {
                Token token = getToken(contact, qq, onNoLoginCall, onInvalidCall);
                if(token == null) return;

                String message = event.getMessage().contentToString();
                Matcher matcher = Pattern.compile("[a-zA-Z0-9]{15}").matcher(message);

                int i = 0;
                while(matcher.find() & ++i < 25) {
                    String code = matcher.group();
                    contact.sendMessage("#%d 小枫在努力兑换 \"%s\" ...".formatted(i, code));
                    Response response = PlayerMusic.gainMusicByCode(token, code);
                    if(response == null) return;
                    if(response.code() == 200) {
                        contact.sendMessage("\"" + code + "\"兑换成功啦！快去背包找找吧");
                        response.close();
                        return;
                    }
                    response.close();
                }
                contact.sendMessage("好像都失效了💦💦\n换几个试试吧！");
            }).build();

    @DeclaredCommand("查找舞立方机台")
    public static final ArgsCommand msgMachineList = new ArgsCommandBuilder()
            .prefix("查找舞立方", "查找", "查找机台", "舞立方")
            .form(ArgsCommand.CHAR)
            .onCall(Scope.GROUP, (event, contact, qq, args) -> {
                if(args == null) return;
                if(args.length == 0) contact.sendMessage("格式：查找舞立方 (地区)");

                String region = args[0];
                if(args[0].length() > 15) return;

                StringBuilder machineListText = new StringBuilder("\"%s\"的舞立方机台列表：".formatted(region));
                List<Machine> list = Machine.getMachineList(region);
                if(list.isEmpty()) {
                    contact.sendMessage("在“" + region + "”似乎没有找到舞立方欸...");
                    return;
                }

                int maxCount = Math.min(list.size(), 5);
                for(int i = 0; i < maxCount; i++) {
                    Machine machine = list.get(i);
                    String show = machine.isShow() ? "[⭐秀]" : "";
                    String online = machine.isOnline() ? "🔵在线" : "🔴离线";
                    String singleInfo = "店名：%s%s %s\n地址：%s\n"
                            .formatted(show, machine.getPlaceName(), online, machine.getAddress());
                    machineListText.append("\n").append(singleInfo);
                }
                if(list.size() > 5) {
                    contact.sendMessage(machineListText + "⭐刷屏哒咩！私聊查询全部" + list.size() + "条~");
                } else {
                    contact.sendMessage(machineListText + "⭐呐！一共" + list.size() + "条~");
                }
            })
            .onCall(Scope.USER, (event, contact, qq, args) -> {
                if(args == null) {
                    contact.sendMessage("格式：\n查找舞立方 (地区)");
                    return;
                }

                String region = args[0];
                StringBuilder machineListText = new StringBuilder("\"%s\"的舞立方机台列表：".formatted(region));
                List<Machine> list = Machine.getMachineList(region);
                if(list.isEmpty()) {
                    contact.sendMessage("似乎没有找到舞立方诶...");
                    return;
                }

                for(Machine machine : list) {
                    String show = machine.isShow() ? "[⭐秀]" : "";
                    String online = machine.isOnline() ? "🔵在线" : "🔴离线";
                    String singleInfo = "店名：%s%s %s\n地址：%s\n".formatted(show, machine.getPlaceName(), online, machine.getAddress());
                    machineListText.append("\n").append(singleInfo);
                }
                contact.sendMessage(machineListText.toString());
            }).build();

    @Deprecated
    @DeclaredCommand("查看其它个人信息")
    public static final ArgsCommand msgOthersInfo = new ArgsCommandBuilder()
            .prefix("看看你的", "康康你的")
            .form(ArgsCommand.NUMBER)
            .onCall(Scope.GLOBAL, (event, contact, qq, args) -> {
                if(args == null) return;

                long num = Long.parseLong(args[0]);
                Token token = getTokenOrDefault(contact, qq, (con, q) ->
                        contact.sendMessage("小枫这登录身份过期了💦\n重新私信登录恢复吧💦"));
                if(token == null) {
//                    contact.sendMessage("默认Token异常，请联系大铃！");
                    return;
                }

                //判断QQ/ID
                int id;
                if(num < 99_999_999 && num > 99_99) { //舞立方ID
                    id = (int) num;
                } else if(userTokensMap.containsKey(num) && num > 999_999) { //QQ
                    id = userTokensMap.get(num).getUserId();
                } else {
                    contact.sendMessage("唔...小枫好像不认识他");
                    return;
                }
                //发送图片
                InputStream inputStream = UserInfoImage.generate(token, id);
                if(inputStream == null) {
                    contact.sendMessage("这个账号未保存或不存在！");
                    return;
                }
                Image image = HttpUtil.getImageFromStream(inputStream, contact);
                contact.sendMessage(image);
            }).build();

    @DeclaredCommand("战力分析")
    public static final RegexCommand msgUserRatio = new RegexCommandBuilder()
            .multiStrings("战力分析", "我的战力", "查看战力", "myrt")
            .onCall(Scope.GLOBAL, (event, contact, qq, args) -> {
                Token token = getToken(contact, qq, onNoLoginCall, onInvalidCall);
                if(token == null) return;

                contact.sendMessage("小枫正在计算中,等一下下💦...");
                InputStream inputStream = UserRatioImage.generate(token);
                Image image;
                if(inputStream != null) {
                    BufferedImage bufferedImage;
                    try(ByteArrayOutputStream baos = new ByteArrayOutputStream()) {
                        Thumbnails.of(inputStream)
                                .scale(1)
                                .outputFormat("jpg").toOutputStream(baos);
                        image = HttpUtil.getImageFromBytes(baos.toByteArray(), contact);
                    } catch(IOException e) {
                        throw new RuntimeException(e);
                    }
                    contact.sendMessage(image);
                }
            }).build();
    @DeclaredCommand("战力分析B30")
    public static final RegexCommand msgUserRatioB30 = new RegexCommandBuilder()
            .multiStrings("战力分析b30", "myrtb30")
            .onCall(Scope.GLOBAL, (event, contact, qq, args) -> {
                Token token = getToken(contact, qq, onNoLoginCall, onInvalidCall);
                if(token == null) return;

                contact.sendMessage("小枫正在计算中,等一下下💦...");
                InputStream inputStream = UserRatioBest30Image.generate(token);
                Image image;
                if(inputStream != null) {
                    BufferedImage bufferedImage;
                    try(ByteArrayOutputStream baos = new ByteArrayOutputStream()) {
                        Thumbnails.of(inputStream)
                                .scale(1)
                                .outputFormat("jpg").toOutputStream(baos);
                        image = HttpUtil.getImageFromBytes(baos.toByteArray(), contact);
                    } catch(IOException e) {
                        throw new RuntimeException(e);
                    }
                    contact.sendMessage(image);
                }
            }).build();
    @DeclaredCommand("战力分析AP30")
    public static final RegexCommand msgUserRatioAP30 = new RegexCommandBuilder()
            .multiStrings("战力分析ap30", "myrtap30")
            .onCall(Scope.GLOBAL, (event, contact, qq, args) -> {
                Token token = getToken(contact, qq, onNoLoginCall, onInvalidCall);
                if(token == null) return;

                contact.sendMessage("小枫正在计算中,等一下下💦...");
                InputStream inputStream = UserRatioAP30Image.generate(token);
                Image image;
                if(inputStream != null) {
                    BufferedImage bufferedImage;
                    try(ByteArrayOutputStream baos = new ByteArrayOutputStream()) {
                        Thumbnails.of(inputStream)
                                .scale(1)
                                .outputFormat("jpg").toOutputStream(baos);
                        image = HttpUtil.getImageFromBytes(baos.toByteArray(), contact);
                    } catch(IOException e) {
                        throw new RuntimeException(e);
                    }
                    contact.sendMessage(image);
                }
            }).build();

    @DeclaredCommand("难度分数列表")
    public static final ArgsCommand levelScores = new ArgsCommandBuilder()
            .prefix("分数列表")
            .form(ArgsCommand.NUMBER)
            .onCall(Scope.GLOBAL, (event, contact, qq, args) -> {
                int level = 1, pages = 1;
                Token token = getToken(contact, qq, onNoLoginCall, onInvalidCall);
                if (token == null) return;

                if (args != null) {
                    try {
                        assert args[0] != null;
                        level = Integer.parseInt(args[0]);
                        if (args.length > 1 && args[1] != null) {
                            pages = Integer.parseInt(args[1]);
                        }
                    } catch (NumberFormatException e) {
                        contact.sendMessage("啊...这个数字是什么");
                        return;
                    }
                }

                contact.sendMessage("小枫正在计算中,等一下下💦...");
                InputStream inputStream = LevelScoresImage.generate(token, level, pages);
                Image image;
                if (inputStream != null) {
                    BufferedImage bufferedImage;
                    try (ByteArrayOutputStream baos = new ByteArrayOutputStream()) {
                        Thumbnails.of(inputStream)
                                .scale(1)
                                .outputFormat("jpg")
                                .toOutputStream(baos);
                        image = HttpUtil.getImageFromBytes(baos.toByteArray(), contact);
                    } catch (IOException e) {
                        throw new RuntimeException(e);
                    }
                    contact.sendMessage(image);
                }
            }).build();


    @DeclaredCommand("ReplyItem") //Todo Beta
    public static final RegexCommand msgReplyItem = new RegexCommandBuilder()
            .regex("myri")
            .onCall(Scope.GLOBAL, (event, contact, qq, args) -> {
                Token token = getToken(contact, qq, onNoLoginCall, onInvalidCall);
                if(token == null) return;
                contact.sendMessage(ReplyItem.get(token).toString());
            }).build();


    @DeclaredCommand("手机号登录")
    public static final ArgsCommand phoneLogin = new ArgsCommandBuilder()
            .prefix("手机号登录", "验证码登录")
            .form(ArgsCommand.NUMBER)
            .onCall(Scope.USER, (event, contact, qq, args) -> {
                if(args == null) {
                    contact.sendMessage("格式：手机号登录 (手机号)\n例：手机号登录 100xxxx0000\n“手机号登录“和(手机号)之间是有空格的。");
                    return;
                }
                String number = args[0];

                PhoneLoginBuilder phoneLoginBuilder = new PhoneLoginBuilder(number);
                InputStream graphCode = phoneLoginBuilder.getGraphCode();
                if(graphCode == null) {
                    contact.sendMessage("无效的手机号！");
                    return;
                }
                Image image = HttpUtil.getImageFromStream(graphCode, contact);
                contact.sendMessage(new PlainText("🐈reCATcha 猫娘检测！\n如果你不是一只猫娘，请发送该识别码")
                        .plus(image).plus("*请勿频繁登录！"));

                EventChannel<Event> channel = getCurrentChannel(event);
                CompletableFuture<MessageEvent> future = new CompletableFuture<>();

                try {
                    while(true) {
                        channel.subscribeOnce(MessageEvent.class, future::complete);
                        String message = future.get(1, TimeUnit.MINUTES).getMessage().contentToString();
                        if(phoneLoginBuilder.getSMSCode(message)) {
                            future = new CompletableFuture<>();
                            break;
                        }

                        if("取消".equals(message)) {
                            contact.sendMessage("登录已取消");
                            return;
                        } else {
                            contact.sendMessage("图形验证码错误，重新发送\n*如需要取消登录请发送“取消”");
                        }
                        future = new CompletableFuture<>();
                    }

                    contact.sendMessage("很棒，你不是一只猫娘！\n验证码已发出，请及时查收并直接发送给舞小枫");

                    Token token;
                    while(true) {
                        channel.subscribeOnce(MessageEvent.class, future::complete);
                        String smsCode = future.get(2, TimeUnit.MINUTES).getMessage().contentToString();
                        if("取消".equals(smsCode)) {
                            contact.sendMessage("登录已取消");
                            return;
                        }
                        token = phoneLoginBuilder.login(smsCode);
                        if(token != null) break;
                        contact.sendMessage("验证码错误，重新发送\n*如需要取消登录请发送“取消”");
                        future = new CompletableFuture<>();
                    }

                    userTokensMap.put(qq, token);
                    contact.sendMessage("登录成功啦~(●'◡'●)\n你的ID是：%s\n\n⭐要是账号不匹配的话，重新登录就好了".formatted(token.getUserId()));
                    contact.sendMessage("欢迎加入机器人的QQ群聊：908659951。");
                } catch(InterruptedException | ExecutionException e) {
                    contact.sendMessage("操作故障，请重试");
                } catch(TimeoutException e) {
                    contact.sendMessage("操作超时，登录已取消");
                }

            }).build();

    /**
     * 获取当前事件/对象所在的通道
     *
     * @param event 事件来源
     * @return 当前通道
     */
    private static @NotNull EventChannel<Event> getCurrentChannel(MessageEvent event) {
        return GlobalEventChannel.INSTANCE.parentScope(DanceFengBot.INSTANCE).filter(getContactFilter(event));
    }


    @DeclaredCommand("登陆")
    public static final RegexCommand fakeLogin = new RegexCommandBuilder()
            .regex("登陆")
            .onCall(Scope.USER, (event, contact, qq, args) -> contact.sendMessage("（生气）你当小枫飞机场啊！登陆登陆的...")).build();

    @Deprecated
    @DeclaredCommand("添加指令")
    public static final ArgsCommand addUserInfoCmd = new ArgsCommandBuilder()
            .prefix("添加指令")
            .form(ArgsCommand.CHAR)
            .onCall(Scope.USER, (event, contact, qq, args) -> {
                if(args == null) {
                    return;
                }
                String newPrefix = args[0];
//                if(!userInfoCommands.containsKey(qq)) userInfoCommands.put(qq, new HashSet<>());
//                userInfoCommands.get(qq).add(newPrefix);
                contact.sendMessage("已添加 \"" + newPrefix + "\" !");
            }).build();

    @Deprecated
    @SuppressWarnings("all")
    @DeclaredCommand("删除指令")
    public static final ArgsCommand delUserInfoCmd = new ArgsCommandBuilder()
            .prefix("删除指令")
            .form(ArgsCommand.CHAR)
            .onCall(Scope.USER, (event, contact, qq, args) -> {
                if(args == null) return;
                HashMap<Long, HashSet<String>> userInfoCommands = new HashMap<>();

                String newPrefix = args[0];
                if(!userInfoCommands.containsKey(qq)) userInfoCommands.put(qq, new HashSet<>());
                if(!userInfoCommands.get(qq).contains(newPrefix)) {
                    contact.sendMessage("未找到 \"" + newPrefix + "\" !");
                    return;
                }
                userInfoCommands.get(qq).remove(newPrefix);
                contact.sendMessage("已删除 \"" + newPrefix + "\" !");
                UserConfigUtils.configsToFile(userInfoCommands, configPath + "UserCommands.json");
            }).build();

    @DeclaredCommand("发送Token JSON")
    public static final RegexCommand showToken = new RegexCommandBuilder()
            .regex("#token")
            .onCall(Scope.GLOBAL, (event, contact, qq, args) -> {
                Token token = getToken(contact, qq, onNoLoginCall, onInvalidCall);
                if(token == null) return;
                if(contact instanceof Group) {
                    contact.sendMessage("私聊才能看的辣！");
                } else {
                    contact.sendMessage(token.toString());
                }
            }).build();

    @DeclaredCommand("发送用户Token JSON")
    public static final ArgsCommand showOthersToken = new ArgsCommandBuilder()
            .prefix("#token")
            .form(ArgsCommand.NUMBER)
            .onCall(Scope.ADMIN, (event, contact, qq, args) -> {
                if(args == null) return;
                Token token = getToken(contact, Long.parseLong(args[0]));
                if(token == null) return;
                if(contact instanceof Group) {
                    contact.sendMessage("私聊才能看的辣！");
                } else {
                    contact.sendMessage(token.toString());
                }
            }).build();


    @DeclaredCommand("发送默认Token JSON")
    public static final RegexCommand showDefaultToken = new RegexCommandBuilder()
            .regex("#token0")
            .onCall(Scope.GLOBAL, (event, contact, qq, args) -> {
                Token token = userTokensMap.get(0L);
                if(token == null) return;
                if(contact instanceof Group) {
                    contact.sendMessage("私聊才能看的辣！");
                } else {
                    contact.sendMessage(token.toString());
                }
            }).build();

    @DeclaredCommand("强制刷新Token")
    public static final RegexCommand refreshToken = new RegexCommandBuilder()
            .regex("#refresh")
            .onCall(Scope.GLOBAL, (event, contact, qq, args) -> {
                Token token = getToken(contact, qq, onNoLoginCall, onInvalidCall);
                if(token == null) return;
                if(contact instanceof Group) {
                    contact.sendMessage("私聊才能用的辣！");
                    return;
                }
                if(token.refresh()) contact.sendMessage("#Token已强制刷新#\n\n" + token);
                else contact.sendMessage("刷新失败，请重新登录！");
            }).build();

    @Deprecated
    @DeclaredCommand("设置默认Token")
    public static final RegexCommand setDefaultToken = new RegexCommandBuilder()
            .regex("#setToken0")
            .onCall(Scope.ADMIN, (event, contact, qq, args) -> {
                contact.sendMessage("请发送 Access Token 和 Refresh Token\n使用换行区分token！");
                EventChannel<Event> channel = GlobalEventChannel.INSTANCE.parentScope(DanceFengBot.INSTANCE);
                CompletableFuture<MessageEvent> future = new CompletableFuture<>();
                channel.subscribeOnce(MessageEvent.class, future::complete);

                String accessToken = null;
                String refreshToken = null;
                try {
                    String[] token = future.get(1, TimeUnit.MINUTES).getMessage().contentToString().trim().split("\n");
                    accessToken = token[0];
                    refreshToken = token[1];
                } catch(InterruptedException | ExecutionException e) {
                    e.printStackTrace();
                } catch(TimeoutException e) {
                    e.printStackTrace();
                    contact.sendMessage("超时了，请重新设置");
                }
                Token token = new Token(0, accessToken, refreshToken, System.currentTimeMillis());
                if(token.checkAvailable()) {
                    defaultToken = token;
                    userTokensMap.put(0L, token);
//                    TokenBuilder.tokensToFile(userTokensMap, configPath + "UserTokens.json");
                    contact.sendMessage("默认Token设置成功：\n\n" + defaultToken);
                } else {
                    contact.sendMessage("默认Token设置失败：已无效");
                }
            }).build();

    @DeclaredCommand("热更新") // TODO 刷新
    public static final ArgsCommand hotUpdate = new ArgsCommandBuilder()
            .prefix("#update")
            .form(Pattern.compile("all|id|reply"))
            .onCall(Scope.ADMIN, (event, contact, qq, args) -> {
                if(args == null) return;
                String param = args[0];

                if(param.equals("all") || param.equals("id")) {
                    contact.sendMessage(TokenBuilder.updateIds().toString());
                    contact.sendMessage("已成功刷新TokenID");
                }
                if(param.equals("all") || param.equals("reply")) {
                    contact.sendMessage("nope...");
                }
            }).build();


    @DeclaredCommand("清空登录等待")//todo 退出登录
    public static final ArgsCommand clearLogin = new ArgsCommandBuilder()
            .prefix("#clearLogin")
            .form(ArgsCommand.WORD)
            .onCall(Scope.USER, (event, contact, qq, args) -> {
                if(args == null) {
                    return;
                }
                // 正在登录检测
                switch(Objects.requireNonNull(args[0])) {
                    case "all" -> logStatus.clear();
                    case "me" -> contact.sendMessage(logStatus.remove(qq) ? "已清空！" : "未找到登录！");
                }
            }).build();


    /**
     * 获取可用 Token (
     * 有了onNull就不要return null了吧...如何处理呢？
     *
     * @param contact 操作对象
     * @param qq      QQ
     * @param onNull  当本地Token==null
     * @return 可用的 Token / defaultToken
     */
    @Deprecated
    public static Token getTokenOrDefault(Contact contact, long qq, @Nullable BiConsumer<Contact, Long> onNull) {
        Token token = userTokensMap.get(qq);

        // 默认返回本地Token
        if(token != null) {

            //默认token有效性
            if(token.checkAvailable()) return token;

            //返回默认token 默认的都null那就登录吧 :(
            if(defaultToken != null && defaultToken.checkAvailable()) return defaultToken;
        }
        //没有登录（本地保存记录）就 onNull.accept();
        if(onNull != null) onNull.accept(contact, qq);

        //甚至连defaultToken==null
        return null;
    }



    public static Token getToken(Contact contact, Long qq) {
        Token token = userTokensMap.get(qq);
        if(token == null || !token.checkAvailable()) {
            // 登录检测
            contact.sendMessage("好像还没有登录诶(´。＿。｀)\n私信发送\"登录\"一起来玩吧！");
//            userInfoCommands.put(qq, new HashSet<>());
            return null;
        }
        return token;
    }



    /**
     * 获取Token
     * <p>*本方法<b>没有提供</b>仅过期但可用的操作，如需请使用{@link Token#checkAvailable()}</p>
     *
     * @param contact   聊天场景
     * @param qq        账号
     * @param onInvalid 本地存在，<b>但不可用/过期</b>时的操作
     * @param onNoLogin 本地不存在时的操作
     * @return 本地Token，无效或不存在时返回null
     */
    @Nullable
    public static Token getToken(Contact contact, long qq, BiConsumer<Contact, Long> onNoLogin, BiConsumer<Contact, Long> onInvalid) {
        Token token = userTokensMap.get(qq);

        // Token不存在
        if(token == null) {
            if(onNoLogin != null) onNoLogin.accept(contact, qq);
            return null;
        }

        // Token存在，但过期
        if(!token.checkAvailable()) {
            if(onInvalid != null) onInvalid.accept(contact, qq);
            return null;
        } else {
            // Token可用
            return token;
        }
    }
}

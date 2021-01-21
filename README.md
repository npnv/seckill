# 秒杀系统（高并发+安全）

[TOC]



# 效果展示

* 用户登录界面

<img src="F:\Typora\Pictures\image-20210115143653561.png" alt="image-20210115143653561" style="zoom: 50%;" />

考虑到张老师提到的安全规则"网页上登录必须加入验证码"，将用户页面引入验证码，暂时没实现老师提到的”3次登录失败后，加入验证码“，验证码的实现具体细节在下面的源码介绍；另外对用户的手机号码进行正则化表达式的校验；



* 秒杀商品列表

<img src="F:\Typora\Pictures\image-20210115145009769.png" alt="image-20210115145009769" style="zoom: 50%;" />

放置四个秒杀商品链接，具体信息可以进入“详情”页、“详情(静态化)”页参与秒杀，详情(静态化)是对前后端分离的一种尝试，将页面直接缓存到客户端。



* 秒杀时间未到

<img src="F:\Typora\Pictures\image-20210115145719385.png" alt="image-20210115145719385" style="zoom:50%;" />

商品设定的时间未到，立即秒杀按钮是无法点击的，这样可以阻止部分用户的疯狂服务器请求。



* 秒杀开始

<img src="F:\Typora\Pictures\image-20210115151717591.png" alt="image-20210115151717591" style="zoom: 50%;" />

秒杀开始，加入数学公式验证码，这样可以避免用户通过明文地址将秒杀请求不停地发送到服务端，同时也有效的防止机器人等手段参与秒杀。



* 秒杀结果

<img src="F:\Typora\Pictures\image-20210115152239846.png" alt="image-20210115152239846" style="zoom:50%;" />

<img src="F:\Typora\Pictures\image-20210115152350721.png" alt="image-20210115152350721" style="zoom:50%;" />

<img src="F:\Typora\Pictures\image-20210115152412095.png" alt="image-20210115152412095" style="zoom:50%;" />

完成秒杀请求，进入商品支付界面。



* 避免重复秒杀

<img src="F:\Typora\Pictures\image-20210115150025487.png" alt="image-20210115150025487" style="zoom:50%;" />

同一个用户秒杀到了两个一样的商品，这种情形也是超卖，应当避免，为此我们将利用数据库本身自带的特性进行防止。



* 秒杀活动结束

<img src="F:\Typora\Pictures\image-20210115152455758.png" alt="image-20210115152455758" style="zoom:50%;" />

秒杀活动结束，不再提供秒杀接口。



# 项目架构

本项目模拟高并发的场景，完成商品的秒杀，同时针对相关的安全规则进行项目优化

<img src="F:\Typora\Pictures\image-20210115155851213.png" alt="image-20210115155851213" style="zoom:50%;" />



# 快速启动

## 数据库

数据设计如下：

<img src="F:\Typora\Pictures\image-20210115164325879.png" alt="image-20210115164325879" style="zoom:50%;" />

* user、goods 、order_info这三张表是正常系统存在的数据表

* seckill_user、seckill_goods、seckill_order三张表是针对秒杀进行设计的

具体的表信息为：

`seckill_user表`

<img src="F:\Typora\Pictures\image-20210115164624203.png" alt="image-20210115164624203" style="zoom:67%;" />

`seckill_goods表`

<img src="F:\Typora\Pictures\image-20210115164717724.png" alt="image-20210115164717724" style="zoom:67%;" />

`seckill_order表`

<img src="F:\Typora\Pictures\image-20210115164749228.png" alt="image-20210115164749228" style="zoom:67%;" />



## 部署步骤

- [ ] 1、克隆仓库到本地

```cmd
git clone https://github.com/charliehxl/seckill.git
```

2、 导入IDEA

3、项目的访问入口：<http://localhost:8080/login/to_login>

初始账号和密码为：`18600000000 / 000000`



# 项目细节

## 安全

### 登录

> 将明文密码进行两次MD5加密

客户端：Client_Password=MD5(明文+固定salt)

服务端：Server_Password=MD5(Client_Password+随机salt)

第一次 （在前端加密，客户端）：密码加密是（明文密码+固定盐值）生成md5用于传输，由于http是明文传输，当输入密码若直接发送服务端验证，此时被截取将直接获取到明文密码，获取用户信息。加salt是为了混淆密码，原则就是明文密码不能在网络上传输。

第二次：在服务端再次加密，当获取到前端发送来的密码后。通过MD5（密码+随机盐值）再次生成密码后存入数据库。防止数据库被盗的情况下，通过md5反查，查获用户密码。方法是盐值会在用户登陆的时候随机生成，并存在数据库中，这个时候就会获取到。

可以看到我们的seckill_user表中也存在一个salt字段，用户登录生成的。

* 前端代码

```javascript
function doLogin() {

        // 获取用户输入密码
        var inputPass = $("#password").val();
        // 获取salt g_passsword_salt = "1a2b3c4d";
        var salt = g_passsword_salt; 
        // md5+salt，与服务器端的第一次MD5规则一致
        var str = "" + salt.charAt(0) + salt.charAt(2) + inputPass + salt.charAt(5) + salt.charAt(4);
        var password = md5(str);

        $.ajax({
            url: "/login/do_login",
            type: "POST",
            data: {
                mobile: $("#mobile").val(),
                password: password
            },
            success: function (data) {
                layer.closeAll();
                if (data.code == 0) {
                    layer.msg("成功");
                    window.location.href = "/goods/to_list";
                } else {
                    layer.msg(data.msg);
                }
                console.log(data);
            },
            error: function () {
                layer.closeAll();
            }
        });
    }
```

* 后端代码

```java
public String login(HttpServletResponse response, LoginVo loginVo) {
    if (loginVo == null) {
        throw new GlobalException(CodeMsg.SERVER_ERROR);
    }
    // 获取用户提交的手机号码和密码
    String mobile = loginVo.getMobile();
    String password = loginVo.getPassword();
    // 判断手机号是否存在
    SeckillUser user = this.getMiaoshaUserById(Long.parseLong(mobile));
    if (user == null)
        throw new GlobalException(CodeMsg.MOBILE_NOT_EXIST);

    // 判断手机号对应的密码是否一致
    String dbPassword = user.getPassword();
    String dbSalt = user.getSalt();
    String calcPass = MD5Util.formPassToDbPass(password, dbSalt);
    if (!calcPass.equals(dbPassword))
        throw new GlobalException(CodeMsg.PASSWORD_ERROR);

    String token = UUIDUtil.uuid();
    redisService.set(SeckillUserKeyPrefix.token, token, user);
    Cookie cookie = new Cookie(COOKIE_NAME_TOKEN, token);
    cookie.setMaxAge(SeckillUserKeyPrefix.token.expireSeconds());
    cookie.setPath("/");
    response.addCookie(cookie);
    return token;
}
```

* 手机号正则化

  -- 简单的正则应用

```java
// 手机号正则
private static final Pattern mobilePattern = Pattern.compile("1\\d{10}");

public static boolean isMobile(String mobile) {
    if (StringUtils.isEmpty(mobile))
        return false;

    Matcher matcher = mobilePattern.matcher(mobile);
    return matcher.matches();
}
```



### 验证码

这种方式主要是防止客户端通过**明文地址+goodsId**将秒杀请求不停地发送到服务端，也有效防止机器人等手段参与秒杀。

* 生成验证码代码

```java
@RequestMapping(value = "/verifyCode", method = RequestMethod.GET)
@ResponseBody
public Result<String> getMiaoshaVerifyCode(HttpServletResponse response, SeckillUser user,@RequestParam("goodsId") long goodsId) {
    if (user == null)
        return Result.error(CodeMsg.SESSION_ERROR);

    // 创建验证码
    try {
        BufferedImage image = seckillService.createVerifyCode(user, goodsId);
        ServletOutputStream out = response.getOutputStream();
        // 将图片写入到resp对象中
        ImageIO.write(image, "JPEG", out);
        out.close();
        out.flush();
        return null;
    } catch (Exception e) {
        e.printStackTrace();
        return Result.error(CodeMsg.SECKILL_FAIL);
    }
}
```

```java
public BufferedImage createVerifyCode(SeckillUser user, long goodsId) {
    if (user == null || goodsId <= 0) {
        return null;
    }

    // 验证码的宽高
    int width = 80;
    int height = 32;

    BufferedImage image = new BufferedImage(width, height,BufferedImage.TYPE_INT_RGB);
    Graphics g = image.getGraphics();
    g.setColor(new Color(0xDCDCDC));
    g.fillRect(0, 0, width, height);
    g.setColor(Color.black);
    g.drawRect(0, 0, width - 1, height - 1);
    Random rdm = new Random();
    for (int i = 0; i < 50; i++) {
        int x = rdm.nextInt(width);
        int y = rdm.nextInt(height);
        g.drawOval(x, y, 0, 0);
    }
    String verifyCode = generateVerifyCode(rdm);
    g.setColor(new Color(0, 100, 0));
    g.setFont(new Font("Candara", Font.BOLD, 24));
    g.drawString(verifyCode, 8, 24);
    g.dispose();

    // 计算表达式值，并把把验证码值存到redis中
    int expResult = calc(verifyCode);
    redisService.set(SeckillKeyPrefix.seckillVerifyCode, user.getId() + "," + goodsId, expResult);
    //输出图片
    return image;
}

private String generateVerifyCode(Random rdm) {
    int num1 = rdm.nextInt(10);
    int num2 = rdm.nextInt(10);
    int num3 = rdm.nextInt(10);
    char op1 = ops[rdm.nextInt(3)];
    char op2 = ops[rdm.nextInt(3)];
    String exp = "" + num1 + op1 + num2 + op2 + num3;
    return exp;
}
```

```java
// 使用ScriptEngine计算验证码中的数学表达式的值
private int calc(String exp) {
    try {
        ScriptEngineManager manager = new ScriptEngineManager();
        ScriptEngine engine = manager.getEngineByName("JavaScript");
        return (Integer) engine.eval(exp);// 表达式计算
    } catch (Exception e) {
        e.printStackTrace();
        return 0;
    }
}

public boolean checkVerifyCode(SeckillUser user, long goodsId, int verifyCode) {
    if (user == null || goodsId <= 0) {
        return false;
    }

    Integer oldCode = redisService.get(SeckillKeyPrefix.seckillVerifyCode, user.getId() + "," + goodsId, Integer.class);
    if (oldCode == null || oldCode - verifyCode != 0) {// !!!!!!
        return false;
    }

    redisService.delete(SeckillKeyPrefix.seckillVerifyCode, user.getId() + "," + goodsId);
    return true;
}
        
```

在服务端计算出验证码的表达式的值，存储在服务端，客户端输入验证码的表达式值，传入服务端进行验证。

- 点击秒杀之前，向让用户输入验证码，分散用户的请求；
- 添加生成验证码的接口；
- 在获取秒杀路径的时候，验证验证码；
- ScriptEngine的使用（用于计算验证码上的表达式）。

当秒杀未开始时，商品详情页异步地向服务端发出获取商品详细信息的请求，同时，获取验证码。服务端收到获取验证码的请求后，生成验证码返回给客户端，同时，将验证码的结果存储在redis中，以便客户端发起秒杀请求时做验证码的校验。



### 秒杀接口隐藏

假使我们将秒杀地址写为静态地址，首先看客户端的秒杀操作逻辑：

```javascript
<button type="button" id="buyButton" onclick="doMiaosha()">立即秒杀</button>

function doMiaosha() {
    $.ajax({
        url: "/miaosha/do_miaosha_static",
        type: "POST",
        data: {
            goodsId: $("#goodsId").val(),
        },
        success: function (data) {
            if (data.code == 0) {
                getMiaoshaResult($("#goodsId").val());
            } else {
                layer.msg(data.msg);
            }
        },
        error: function () {
            layer.msg("客户端请求有误");
        }
    });
}
```

这样的话，用户点击秒杀按钮后，会向服务端请求秒杀商品的秒杀信息，客户端的POST请求是以明文的方式发送给服务器的，如果使用一种工具将POST请求体中的数据和请求的URL组合起来，构成一个完整的POST请求，然后不停地向服务器请求资源，则会给服务器带来很大的压力，同时，这样一种作弊的方式带来的用户体验也是极差的，这样一种设计缺陷会被别有用心的人用于不正当交易，因此，需要一种方式克服这种缺陷，这就引出了秒杀接口的隐藏。

**实现秒杀接口地址的隐藏**

在秒杀开始之前，秒杀接口地址不要写到客户端，而是在秒杀开始之后，将秒杀地址动态地在客户端和服务器间进行交互完成拼接。这样一来，秒杀开始之前，秒杀地址对客户端不可见。

* 后端代码

```java
public Result<String> getMiaoshaPath(Model model, SeckillUser user,@RequestParam("goodsId") long goodsId,@RequestParam(value = "verifyCode", defaultValue = "0") int verifyCode) {

    model.addAttribute("user", user);

    if (user == null) {
        return Result.error(CodeMsg.SESSION_ERROR);
    }

    // 校验验证码
    boolean check = seckillService.checkVerifyCode(user, goodsId, verifyCode);
    if (!check)
        return Result.error(CodeMsg.REQUEST_ILLEGAL);// 检验不通过，请求非法

    // 检验通过，获取秒杀路径
    String path = seckillService.createSeckillPath(user, goodsId);
    // 向客户端回传随机生成的秒杀地址
    return Result.success(path);
}
```

```java
//goodsId和verifyCode两个参数进行
public String createSeckillPath(SeckillUser user, long goodsId) {
    if (user == null || goodsId <= 0) {
        return null;
    }
    // 随机生成秒杀地址
    String path = MD5Util.md5(UUIDUtil.uuid() + "123456");
    redisService.set(SeckillKeyPrefix.seckillPath, "" + user.getId() + "_" + goodsId, path);
    return path;
}
```

```java
public boolean checkPath(SeckillUser user, long goodsId, String path) {
    if (user == null || path == null)
        return false;
    // 从redis中读取出秒杀的path变量是否为本次秒杀操作执行前写入redis中的path
    String oldPath = redisService.get(SeckillKeyPrefix.seckillPath, "" + user.getId() + "_" + goodsId, String.class);
    return path.equals(oldPath);
}
```

* 前端代码
```javascript
/*秒杀接口隐藏*/
function getSeckillPath() {
    var goodsId = $("#goodsId").val();
    g_showLoading();
    $.ajax({
        url: "/miaosha/path",
        type: "GET",
        data: {
            goodsId: goodsId,
            verifyCode: $("#verifyCode").val()
        },
        success: function (data) {
            if (data.code == 0) {
                var path = data.data;
                doMiaosha(path);
            } else {
                layer.msg(data.msg);
            }
        },
        error: function () {
            layer.msg("客户端请求有误");
        }
    });
}

/*真正做秒杀的接口, path为服务端返回的秒杀接口地址*/
function doMiaosha(path) {
    $.ajax({
        url: "/miaosha/" + path + "/do_miaosha_static",
        type: "POST",
        data: {
            goodsId: $("#goodsId").val()
        },
        success: function (data) {
            if (data.code == 0) {
                getMiaoshaResult($("#goodsId").val());
            } else {
                layer.msg(data.msg);
            }
        },
        error: function () {
            layer.msg("客户端请求有误");
        }
    });
}

/*获取秒杀的结果*/
function getMiaoshaResult(goodsId) {
    $.ajax({
        url: "/miaosha/result",
        type: "GET",
        data: {
            goodsId: $("#goodsId").val(),
        },
        success: function (data) {
            if (data.code == 0) {
                var result = data.data;
                if (result < 0) {
                    layer.msg("对不起，秒杀失败");
                } else if (result == 0) {
                    setTimeout(function () {
                        getMiaoshaResult(goodsId);
                    }, 200);
                } else {
                    layer.confirm("恭喜你，秒杀成功！查看订单？", {btn: ["确定", "取消"]},
                                  function () {
                        window.location.href = "/order_detail.htm?orderId=" + result;
                    },
                                  function () {
                        layer.closeAll();
                    });
                }
            } else {
                layer.msg(data.msg);
            }
        },
        error: function () {
            layer.msg("客户端请求有误");
        }
    });
}
```



## 性能优化

### 秒杀引入RabbitMQ

针对秒杀系统，最大的挑战就是请求的骤增。假设系统A在某一段时间请求数暴增，有5000个请求发送过来，系统A这时就会发送5000条SQL进入MySQL进行执行，MySQL对于如此庞大的请求当然处理不过来，MySQL就会崩溃，导致系统瘫痪。



使用消息队列主要有三个作用：

1、解耦 2、异步 3、削峰

RabbitMQ的整体架构

<img src="F:\Typora\Pictures\image-20210119164122852.png" alt="image-20210119164122852" style="zoom:50%;" />

RabbitMQ的组成：

- Broker：消息队列服务进程。此进程包括两个部分：Exchange和Queue。
- Exchange：消息队列交换机。**按一定的规则将消息路由转发到某个队列**。
- Queue：消息队列，存储消息的队列。
- Producer：消息生产者。生产方客户端将消息同交换机路由发送到队列中。
- Consumer：消息消费者。消费队列中存储的消息。

协同工作大概的流程为：

<img src="F:\Typora\Pictures\image-20210119164428218.png" alt="image-20210119164428218" style="zoom:50%;" />

在项目的具体应用：

```java
//在MQ中传递的秒杀信息 -- 包含参与秒杀的用户和商品的id
//将用户秒杀信息投递到MQ中（使用direct模式的exchange）
public void sendMiaoshaMessage(SeckillMessage message) {
    String msg = RedisService.beanToString(message);
    logger.info("MQ send message: " + msg);
    // 第一个参数为消息队列名，第二个参数为发送的消息
    amqpTemplate.convertAndSend(MQConfig.SECKILL_QUEUE, msg);
}
```

```java
//处理收到的秒杀成功信息
@RabbitListener(queues = MQConfig.SECKILL_QUEUE)
public void receiveMiaoshaInfo(String message) {
    logger.info("MQ: message: " + message);
    SeckillMessage seckillMessage = RedisService.stringToBean(message,SeckillMessage.class);
    // 获取秒杀用户信息与商品id
    SeckillUser user = seckillMessage.getUser();
    long goodsId = seckillMessage.getGoodsId();

    // 获取商品的库存
    GoodsVo goods = goodsService.getGoodsVoByGoodsId(goodsId);
    Integer stockCount = goods.getStockCount();
    if (stockCount <= 0)
        return;

    SeckillOrder order = orderService.getSeckillOrderByUserIdAndGoodsId(user.getId(), goodsId);
    if (order != null)
        return;

    // 减库存 下订单 写入秒杀订单
    seckillService.seckill(user, goods);
}
```



### 秒杀引入Redis

Redis是什么？

Redis: Remote Dictionary Server(远程字典服务器)
是完全开源免费的，用C语言编写的，遵守BSD协议，是一个高性能的(key/value)分布式内存数据库，基于内存运行并支持持久化的NoSQL数据库，是当前最热门的NoSql数据库之一,也被人们称为数据结构服务器。

Redis 与其他 key - value 缓存产品有以下三个特点：

- Redis支持数据的持久化，可以将内存中数据保持在磁盘中，重启的时候可以再次加载进行使用
- Redis不仅仅支持简单的key-value类型的数据，同时还提供list，set，zset，hash等数据结构的存储。
- Redis支持数据的备份，即master-slave模式的数据备份

具体的使用功能有以下：

- 内存存储和持久化：redis支持异步将内存中的数据写到硬盘上，同时不影响继续服务
- 取最新N个数据的操作，如：可以将最新的10条评论的ID放在Redis的List集合里面
- 模拟类似于HttpSession这种需要设定过期时间的功能
- 发布、订阅消息系统
- 定时器、计数器



针对具体的项目操作：

1、Redis预减库存减少数据库的访问

在做秒杀时，需要先查询数据库中的商品库存，确保逻辑正确，在本项目中，我们将库存信息信息存储在redis中，从而可以减少对数据库的访问。

```java
//redis 的get操作，通过key获取存储在redis中的对象
public <T> T get(KeyPrefix prefix, String key, Class<T> clazz) {
    Jedis jedis = null;// redis连接

    try {
        jedis = jedisPool.getResource();
        String realKey = prefix.getPrefix() + key;
        String strValue = jedis.get(realKey);
        T objValue = stringToBean(strValue, clazz);
        return objValue;
    } finally {
        returnToPool(jedis);
    }
}
```

```java
// redis的set操作
public <T> boolean set(KeyPrefix prefix, String key, T value) {
    Jedis jedis = null;
    try {
        jedis = jedisPool.getResource();
        String strValue = beanToString(value);
        if (strValue == null || strValue.length() <= 0)
            return false;
        String realKey = prefix.getPrefix() + key;
        int seconds = prefix.expireSeconds();

        if (seconds <= 0) {
            jedis.set(realKey, strValue);
        } else {
            jedis.setex(realKey, seconds, strValue);
        }
        return true;
    } finally {
        returnToPool(jedis);
    }
}
```

```java
// 判断key是否存在于redis中
public <T> boolean exists(KeyPrefix keyPrefix, String key) {
    Jedis jedis = null;
    try {
        jedis = jedisPool.getResource();
        String realKey = keyPrefix.getPrefix() + key;
        return jedis.exists(realKey);
    } finally {
        returnToPool(jedis);
    }
}
```

```java
//自增
public <T> Long incr(KeyPrefix keyPrefix, String key) {
        Jedis jedis = null;
        try {
            jedis = jedisPool.getResource();
            String realKey = keyPrefix.getPrefix() + key;
            return jedis.incr(realKey);
        } finally {
            returnToPool(jedis);
        }
    }

//自减
public <T> Long decr(KeyPrefix keyPrefix, String key) {
        Jedis jedis = null;
        try {
            jedis = jedisPool.getResource();
            String realKey = keyPrefix.getPrefix() + key;
            return jedis.decr(realKey);
        } finally {
            returnToPool(jedis);
        }
    }
```

```java
//删除缓存中的用户数据
public boolean delete(KeyPrefix prefix, String key) {
        Jedis jedis = null;
        try {
            jedis = jedisPool.getResource();
            String realKey = prefix.getPrefix() + key;
            Long del = jedis.del(realKey);
            return del > 0;
        } finally {
            returnToPool(jedis);
        }
    }
```

```java
//将对象转换为对应的json字符串
public static <T> String beanToString(T value) {
        if (value == null)
            return null;

        Class<?> clazz = value.getClass();
        /*首先对基本类型处理*/
        if (clazz == int.class || clazz == Integer.class)
            return "" + value;
        else if (clazz == long.class || clazz == Long.class)
            return "" + value;
        else if (clazz == String.class)
            return (String) value;
            /*然后对Object类型的对象处理*/
        else
            return JSON.toJSONString(value);
    }
```

```java
// 根据传入的class参数，将json字符串转换为对应类型的对象
public static <T> T stringToBean(String strValue, Class<T> clazz) {

        if ((strValue == null) || (strValue.length() <= 0) || (clazz == null))
            return null;

        // int or Integer
        if ((clazz == int.class) || (clazz == Integer.class))
            return (T) Integer.valueOf(strValue);
            // long or Long
        else if ((clazz == long.class) || (clazz == Long.class))
            return (T) Long.valueOf(strValue);
            // String
        else if (clazz == String.class)
            return (T) strValue;
            // 对象类型
        else
            return JSON.toJavaObject(JSON.parseObject(strValue), clazz);
    }

```



## 其他

### 页面静态化，前后端分离

页面静态化指的是将页面直接缓存到客户端。常用的技术有`Angular.js`，`Vue.js`。

其实现方式就是通过`ajax`异步请求服务器获取动态数据，对于非动态数据部分缓存在客户端，客户端通过获取服务端返回的`json`数据解析完成相应的逻辑。

在本项目中，我们对**商品详情页**和**订单详情页**做了一个静态化处理。

对于商品详情页，异步地从服务端获取商品详情信息，然后客户端完成页面渲染工作。除此之外，对于秒杀信息的获取也是通过异步获取完成的。例如，当秒杀开始时，用户执行秒杀动作，客户端就会**轮询**服务器获取秒杀结果。而不需要服务器直接返回页面。

而对于订单详情页，实际上也是同样的思路。

```javascript
//使用ajax从服务端请求页面数据, 跳转到这个页面时就会执行(function有$)
$(function () {
    // countDown();
    getDetail();// 获取商品详情
});

function getDetail() {
    var goodsId = g_getQueryString("goodsId");// goodsId为goods_list.html中详情url中的参数
    $.ajax({
        url: "/goods/to_detail_static/" + goodsId,
        type: "GET",
        success: function (data) {// data为edu.uestc.controller.GoodsListController#toDetailStatic的返回值
            if (data.code == 0) {
                render(data.data);
            } else {
                layer.msg(data.msg);
            }
        },
        error: function () {
            layer.msg("客户端请求有误");
        }
    });
}

/*渲染页面*/
function render(detail) {
    var seckillStatus = detail.seckillStatus;
    var remainSeconds = detail.remainSeconds;
    var goods = detail.goods;
    var user = detail.user;
    if (user) {
        $("#userTip").hide();
    }
    $("#goodsName").text(goods.goodsName);
    $("#goodsImg").attr("src", goods.goodsImg);
    $("#startTime").text(new Date(goods.startDate).format("yyyy-MM-dd hh:mm:ss"));
    $("#remainSeconds").val(remainSeconds);
    $("#goodsId").val(goods.id);
    $("#goodsPrice").text(goods.goodsPrice);
    $("#seckillPrice").text(goods.seckillPrice);
    $("#stockCount").text(goods.stockCount);
    countDown();
}

function countDown() {
    var remainSeconds = $("#remainSeconds").val();
    var timeout;
    if (remainSeconds > 0) {//秒杀还没开始，倒计时
        $("#buyButton").attr("disabled", true);
        $("#miaoshaTip").html("秒杀倒计时：" + remainSeconds + "秒");
        timeout = setTimeout(function () {
            $("#countDown").text(remainSeconds - 1);
            $("#remainSeconds").val(remainSeconds - 1);
            countDown();
        }, 1000);
    } else if (remainSeconds == 0) {//秒杀进行中
        $("#buyButton").attr("disabled", false);
        if (timeout) {
            clearTimeout(timeout);
        }
        $("#miaoshaTip").html("秒杀进行中");
        // 在倒计时结束时获取验证码（使用ajax异步向服务器请求验证码图片）
        $("#verifyCodeImg").attr("src", "/miaosha/verifyCode?goodsId=" + $("#goodsId").val());
        $("#verifyCodeImg").show();// 从服务器加载完验证码图片后，显示出来
        $("#verifyCode").show();
    } else {//秒杀已经结束
        $("#buyButton").attr("disabled", true);
        $("#miaoshaTip").html("秒杀已经结束");
        $("#verifyCodeImg").hide();
        $("#verifyCode").hide();
    }
}
```



### 超卖问题

超卖问题实际上是两个问题：

1. 商品的库存减为负数，也就出现了超卖问题，这是不合理的；
1. 同一个用户秒杀到了两个一样的商品，这种情形也是超卖，应当避免。

来看看两个问题出现的情形：

对于**第一个问题**，我们知道，秒杀需要执行两个关键的操作，第一个是从数据库减库存，第二个是生成订单到插入到数据库，这两个操作共同构成了秒杀操作，因此，**秒杀操作是一个事务**。

```java
@Transactional
public OrderInfo miaosha(MiaoshaUser user, GoodsVo goods) {
    // 1. 减库存
    boolean success = goodsService.reduceStock(goods);
    if (!success) {
        setGoodsOver(goods.getId());
        return null;
    }
    // 2. 生成订单；向order_info表和maiosha_order表中写入订单信息
    return orderService.createOrder(user, goods);
}
```

如果商品是由10个，而达到`goodsService.reduceStock()`的请求有100个，当它们同时执行减库存操作时，会导致库存变为-90，这就引发了超卖问题。如何解决呢？

来看看减库存操作的数据库的`Mapper`

```java
@Update("UPDATE miaosha_goods SET stock_count = stock_count-1 WHERE goods_id=#{goodsId}")
int reduceStack(MiaoshaGoods miaoshaGoods);
```

因为每次`UPDATE`对于数据库来说都是原子的，如果每次减库存操作之前先判断库存是否大于零，则可以利用数据库层面的原子性来保证库存不会为负数，这也就解决了超卖的问题。

```java
@Update("UPDATE miaosha_goods SET stock_count = stock_count-1 WHERE goods_id=#{goodsId} AND stock_count > 0")
int reduceStack(MiaoshaGoods miaoshaGoods);
```

`AND stock_count > 0`即为数据库层面解决超卖的保证。

对于**第二个问题**，考虑一种情形，如果一个未秒杀成功的用户**同时**对一个商品发出两次秒杀请求，对于两次秒杀请求，服务器层面会判断用户的两次秒杀请求为合法请求，然后完成从数据库减库存和将订单插入到数据库的操作，显然，这是不合理的。因为一个用户只能秒杀一个商品，如果执行成功，则订单表中会出现两条条商品id和用户id相同的记录，一个商品的库存被同一个用户减了两次（也可能是多次），这就引发了超卖问题。

因此，为了解决这个问题，我们要充分利用事务的特性。从数据库减库存和将订单记录插入到数据库构成了事务，如果一个操作未执行成功，则事务会回滚。如果我们对`miaosha_order`中的`user_id`和`goods_id`字段创建一个联合**唯一索引**，则在插入两条`user_id`和`goods_id`相同的记录时，将会操作失败，从而事务回滚，秒杀不成功，这就解决了同一个用户发起对一个商品同时发起多次请求引发的超卖问题。

至此，通过上述的分析，超卖问题就可以得到解决了。总结起来如下。

**总结：**

- SQL加库存数量的判断：防止库存变为负数；
- 数据库加唯一**索引**：防止用户重复购买。





# 项目与课程总结

​		整个学期课程学下来，收获满满。因为解决课程不会的问题，认识了很多厉害的同学，同时也为我一个跨考生打开了开发的“冰山一角”，在此很感谢张老师的课程细心安排，另外针对同学的实习也尽心尽力。

​		针对该项目，也存在很多待优化的地方，希望能得到老师的指导。










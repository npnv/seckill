package com.pkchar.dao;

import com.pkchar.domain.SeckillGoods;
import com.pkchar.vo.GoodsVo;
import org.apache.ibatis.annotations.Mapper;
import org.apache.ibatis.annotations.Param;
import org.apache.ibatis.annotations.Select;
import org.apache.ibatis.annotations.Update;

import java.util.List;

/**
 * goods 表的数据库访问层
 */
@Mapper
public interface GoodsDao {


    /**
     * 查出商品信息（包含该商品的秒杀信息）
     * 利用左外连接(LEFT JOIN...ON...)的方式查
     *
     * @return
     */
    @Select("SELECT g.*, mg.stock_count, mg.start_date, mg.end_date, mg.seckill_price FROM seckill_goods mg LEFT JOIN goods g ON mg.goods_id=g.id")
    List<GoodsVo> listGoodsVo();

    /**
     * 通过商品的id查出商品的所有信息（包含该商品的秒杀信息）
     *
     * @param goodsId
     * @return
     */
    @Select("SELECT g.*, mg.stock_count, mg.start_date, mg.end_date, mg.seckill_price FROM seckill_goods mg LEFT JOIN goods g ON mg.goods_id=g.id where g.id = #{goodsId}")
    GoodsVo getGoodsVoByGoodsId(@Param("goodsId") Long goodsId);

    /**
     * 减少seckill_order中的库存
     * <p>
     * c5: 增加库存判断 stock_count>0, 一次使得数据库不存在卖超问题
     *
     * @param seckillGoods
     */
    @Update("UPDATE seckill_goods SET stock_count = stock_count-1 WHERE goods_id=#{goodsId} AND stock_count > 0")
    int reduceStack(SeckillGoods seckillGoods);
}

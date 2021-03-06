package com.wan.sys.service.cityManager.impl;

import java.io.BufferedReader;
import java.io.BufferedWriter;
import java.io.File;
import java.io.FileInputStream;
import java.io.FileOutputStream;
import java.io.FileWriter;
import java.io.IOException;
import java.io.InputStreamReader;
import java.io.OutputStreamWriter;
import java.io.PrintWriter;
import java.text.SimpleDateFormat;
import java.util.*;

import com.wan.sys.entity.photo.PhotoType;
import org.apache.commons.lang.StringUtils;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

import com.wan.sys.common.GlobalContext;
import com.wan.sys.dao.common.IBaseDao;
import com.wan.sys.entity.User;
import com.wan.sys.entity.banner.Banner;
import com.wan.sys.entity.cityManager.Fixed;
import com.wan.sys.entity.cityManager.PartToState;
import com.wan.sys.entity.cityManager.Position;
import com.wan.sys.entity.comment.Comment;
import com.wan.sys.entity.dynamic.Dynamic;
import com.wan.sys.entity.forum.Forum;
import com.wan.sys.entity.guide.Guide;
import com.wan.sys.entity.image.Image;
import com.wan.sys.entity.lost.Lost;
import com.wan.sys.entity.message.Message;
import com.wan.sys.entity.photo.Handle;
import com.wan.sys.entity.photo.Photo;
import com.wan.sys.pojo.CityBean;
import com.wan.sys.pojo.DataGridBean;
import com.wan.sys.pojo.DataGridJson;
import com.wan.sys.pojo.Json;
import com.wan.sys.service.cityManager.IcityPageManagerService;
import com.wan.sys.service.common.impl.CommonServiceImpl;
import com.wan.sys.util.ConfigUtil;
import com.wan.sys.util.Encrypt;
import com.wan.sys.util.StringUtil;

@Service
public class cityPageServiceImpl extends CommonServiceImpl implements IcityPageManagerService {

    @Autowired
    IBaseDao baseDao;

    @Override
    public DataGridJson positionList(DataGridBean dg, CityBean city) {
        DataGridJson j = new DataGridJson();
        String hql = " from Position p where recordStatus='Y' ";
        if (StringUtils.isNotBlank(city.getPositionName())) {
            hql += " and p.name like '%%" + city.getPositionName() + "%%'";
        }
        String totalHql = " select count(*) " + hql;
        j.setTotal(baseDao.count(totalHql));
        if (dg.getOrder() != null) {
            hql += " order by createTime desc";
        }
        List<Position> positionList = baseDao.find(hql, dg.getPage(), dg.getRows());
        j.setRows(positionList);
        return j;
    }

    @Override
    public Boolean alterPosition(CityBean city) {
        if (StringUtils.isNotBlank(city.getPositionId())) {//编辑
            Position p = (Position) baseDao.get(Position.class, Long.valueOf(city.getPositionId()));
            p.setName(city.getPositionName());
            p.setLongitude(city.getLongitude());
            p.setLatitude(city.getLatitude());
            p.setType(city.getPositionType());
            p.setModifyTime(new Date());
            p.setModifyUserId(GlobalContext.getCurrentUser().getId());
            baseDao.update(p);
            return false;
        } else {//添加，地点名不能重复
            Position p = new Position();
            p.setName(city.getPositionName());
            p.setLongitude(city.getLongitude());
            p.setLatitude(city.getLatitude());
            p.setType(city.getPositionType());
            p.setRecordStatus("Y");
            p.setCreateTime(new Date());
            p.setCreateUserId(GlobalContext.getCurrentUser().getId());
            p.setCreateUserName(GlobalContext.getCurrentUser().getNickName());
            baseDao.save(p);
            return true;
        }
    }

    @Override
    public Boolean removePositions(String[] id) {
        String ids = "";
        for (int i = 0; i < id.length; i++) {
            if (i == id.length - 1) {
                ids += id[i];
            } else {
                ids += id[i] + ",";
            }
        }
        baseDao.executeSql(" update `city_position` set RECORDSTATUS='N' where ID in(" + ids + ")");
        return true;
    }

    @Override
    public DataGridJson messageList(DataGridBean dg, CityBean city) {
        DataGridJson j = new DataGridJson();
        String hql = " from Message  where recordStatus='Y' ";
        if (StringUtils.isNotBlank(city.getMessageTitle())) {
            hql += " and title like '%%" + city.getMessageTitle() + "%%'";
        }
        if (StringUtils.isNotBlank(city.getMessageIsOnline())) {
            hql += " and isOnline='" + city.getMessageIsOnline() + " '";
        }
        if (StringUtil.isNotBlank(city.getStartTime())) {
            hql += " and createTime>'" + city.getStartTime() + "'";
        }
        if (StringUtil.isNotBlank(city.getEndTime())) {
            hql += " and createTime<'" + city.getEndTime() + "'";
        }
        String totalHql = " select count(*) " + hql;
        j.setTotal(baseDao.count(totalHql));
        if (dg.getOrder() != null) {
            hql += " order by createTime  desc";
        }
        List<Message> positionList = baseDao.find(hql, dg.getPage(), dg.getRows());
        j.setRows(positionList);
        return j;
    }

    @Override
    public Boolean alterMessage(CityBean city) {
        if (StringUtils.isNotBlank(city.getMessageId())) {//编辑，不支持更改封面图片，其他图片存在html
            //改为可以更改封面，先删除再添加
            baseDao.executeSql("delete from city_part_to_images where TYPE='1' and BELONG_ID=" + city.getMessageId());
            if (StringUtils.isNotBlank(city.getMessageImage())) {
                Image i = new Image();
                i.setBelongId(Long.valueOf(city.getMessageId()));
                i.setType("1");
                i.setAddress(city.getMessageImage());
                baseDao.save(i);
            }

            Message p = (Message) baseDao.get(Message.class, Long.valueOf(city.getMessageId()));
            p.setTitle(city.getMessageTitle());
            p.setSubtitle(city.getMessageSubTitle());
            p.setContent(city.getMessageContent());
            p.setModifyTime(new Date());
            p.setModifyUserId(GlobalContext.getCurrentUser().getId());
            baseDao.update(p);
            //h5
            city.setMessageId(p.getId().toString());
            try {
                p.setH5url(h5url(city));
            } catch (IOException e) {
                // TODO Auto-generated catch block
                e.printStackTrace();
            }
            return false;
        } else {//添加
            Message p = new Message();
            p.setTitle(city.getMessageTitle());
            p.setSubtitle(city.getMessageSubTitle());
            p.setContent(city.getMessageContent());
            p.setIsOnline("N");
            p.setRecordStatus("Y");
            p.setCreateTime(new Date());
            p.setCreateUserName(GlobalContext.getCurrentUser().getNickName());
            p.setCreateUserId(GlobalContext.getCurrentUser().getId());
            baseDao.save(p);
            //h5
            city.setMessageId(p.getId().toString());
            try {
                p.setH5url(h5url(city));
            } catch (IOException e) {
                // TODO Auto-generated catch block
                e.printStackTrace();
            }
            //添加封面
            if (StringUtils.isNotBlank(city.getMessageImage())) {
                Image i = new Image();
                i.setBelongId(p.getId());
                i.setType("1");
                i.setAddress(city.getMessageImage());
                baseDao.save(i);
            }


            return true;
        }
    }

    @Override
    public Boolean removeMessages(String[] id) {
        String ids = "";
        for (int i = 0; i < id.length; i++) {
            if (i == id.length - 1) {
                ids += id[i];
            } else {
                ids += id[i] + ",";
            }
        }
        baseDao.executeSql(" update `city_message` set RECORDSTATUS='N' where ID in(" + ids + ")");
        return true;
    }


    @Override
    public Boolean changeMessages(String[] id) {
        String ids = "";
        for (int i = 0; i < id.length; i++) {
            if (i == id.length - 1) {
                ids += id[i];
            } else {
                ids += id[i] + ",";
            }
        }
        baseDao.executeSql(" UPDATE `city_message` SET IS_ONLINE = CASE IS_ONLINE  WHEN 'Y' THEN 'N' WHEN 'N' THEN 'Y' END WHERE  ID in(" + ids + ")");
        return true;
    }

    @Override
    public List<Fixed> positionTypeSelect() {
        List<Fixed> list = baseDao.find(" from Fixed where type='position' ");
        return list;
    }

    @Override
    public DataGridJson photoList(DataGridBean dg, CityBean city) {
        DataGridJson j = new DataGridJson();
        String hql = " from Photo where 1=1";
        if (StringUtil.isNotBlank(city.getPhotoName())) {
            hql += " and content like '%%" + city.getPhotoName() + "%%'";
        }
        if (StringUtil.isNotBlank(city.getState())) {
            hql += " and state='" + city.getState() + "'";
        }
        if (StringUtil.isNotBlank(city.getStartTime())) {
            hql += " and createTime>'" + city.getStartTime() + "'";
        }
        if (StringUtil.isNotBlank(city.getEndTime())) {
            hql += " and createTime<'" + city.getEndTime() + "'";
        }
        if (city.getType() != null) {
            hql += " and type= " + city.getType();
        }
        if (StringUtil.isNotBlank(city.getUserName())) {
            hql += " and createUserName.nickName='" + city.getUserName() + "'";
        }
        if (StringUtil.isNotBlank(city.getUserPhone())) {
            hql += " and createUserName.mobilePhone='" + city.getUserPhone() + "'";
        }
        if (StringUtil.isNotBlank(city.getUserDel())) {
            if (city.getUserDel().equals("1")) {//管理员删除
                hql += " and recordStatus='N' and userDel='Y'";
            } else if (city.getUserDel().equals("2")) {//用户删除
                hql += " and userDel='N' and recordStatus='Y'";
            } else if (city.getUserDel().equals("3")) {
                hql += " and userDel='N' and recordStatus='N'";
            } else if (city.getUserDel().equals("0")) {
                hql += " and userDel='Y' and recordStatus='Y'";
            } else if (city.getUserDel().equals("4")) {
                hql += " ";
            }
        } else {//默认为正常
            hql += " and userDel='Y' and recordStatus='Y'";
        }
        String totalHql = " select count(*) " + hql;
        j.setTotal(baseDao.count(totalHql));
        if (dg.getOrder() != null) {
            hql += " order by  createTime desc";
        }
        List<Photo> photoList = baseDao.find(hql, dg.getPage(), dg.getRows());
        j.setRows(photoList);
        return j;
    }

    @Override
    public List<Fixed> photoStateSelect() {
        List<Fixed> list = baseDao.find(" from Fixed where type='photo' ");
        return list;
    }

    @Override
    public Boolean changePhotoState(CityBean city) {
        PartToState p = new PartToState();
        p.setBelongId(Long.valueOf(city.getPhotoId()));
        p.setName(city.getState());
        p.setContent(city.getRemark());
        p.setCreateTime(new Date());
        p.setCreateUserId(GlobalContext.getCurrentUser().getId());
        baseDao.save(p);
        Photo t = (Photo) baseDao.get(Photo.class, Long.valueOf(city.getPhotoId()));
        if (t != null) {
            t.setState(city.getCode());
            baseDao.update(t);
        }
        //保存处理图片
        if (StringUtil.isNotBlank(city.getMessageImage())) {
            String[] imgs = city.getMessageImage().split(",");
            for (int i = 0; i < imgs.length; i++) {
                Handle h = new Handle();
                h.setBelongId(Long.valueOf(city.getPhotoId()));
                h.setAddress(imgs[i]);
                baseDao.save(h);
            }
        }
        return true;
    }

    @Override
    public DataGridJson dynamicList(DataGridBean dg, CityBean city) {
        DataGridJson j = new DataGridJson();
        String hql = " from Dynamic  where recordStatus='Y' ";
        if (StringUtils.isNotBlank(city.getMessageTitle())) {
            hql += " and title like '%%" + city.getMessageTitle() + "%%'";
        }
        if (StringUtils.isNotBlank(city.getMessageIsOnline())) {
            hql += " and isOnline='" + city.getMessageIsOnline() + " '";
        }
        if (StringUtil.isNotBlank(city.getStartTime())) {
            hql += " and createTime>'" + city.getStartTime() + "'";
        }
        if (StringUtil.isNotBlank(city.getEndTime())) {
            hql += " and createTime<'" + city.getEndTime() + "'";
        }
        String totalHql = " select count(*) " + hql;
        j.setTotal(baseDao.count(totalHql));
        if (dg.getOrder() != null) {
            hql += " order by createTime  desc";
        }
        List<Dynamic> dynamicList = baseDao.find(hql, dg.getPage(), dg.getRows());
        j.setRows(dynamicList);
        return j;
    }

    @Override
    public Boolean alterDynamic(CityBean city) {
        if (StringUtils.isNotBlank(city.getMessageId())) {//编辑，不支持更改封面图片，其他图片存在html
            //改为可以更改封面，先删除再添加
            baseDao.executeSql("delete from city_part_to_images where TYPE='3' and  BELONG_ID=" + city.getMessageId());
            if (StringUtils.isNotBlank(city.getMessageImage())) {
                Image i = new Image();
                i.setBelongId(Long.valueOf(city.getMessageId()));
                i.setType("3");
                i.setAddress(city.getMessageImage());
                baseDao.save(i);
            }

            Dynamic p = (Dynamic) baseDao.get(Dynamic.class, Long.valueOf(city.getMessageId()));
            p.setTitle(city.getMessageTitle());
            p.setSubtitle(city.getMessageSubTitle());
            p.setContent(city.getMessageContent());
            p.setModifyTime(new Date());
            p.setModifyUserId(GlobalContext.getCurrentUser().getId());
            baseDao.update(p);
            //h5
            city.setMessageId(p.getId().toString());
            try {
                p.setH5url(h5url(city));
            } catch (IOException e) {
                // TODO Auto-generated catch block
                e.printStackTrace();
            }
            return false;
        } else {//添加
            Dynamic p = new Dynamic();
            p.setTitle(city.getMessageTitle());
            p.setSubtitle(city.getMessageSubTitle());
            p.setContent(city.getMessageContent());
            p.setIsOnline("N");
            p.setRecordStatus("Y");
            p.setCreateTime(new Date());
            p.setCreateUserName(GlobalContext.getCurrentUser().getNickName());
            p.setCreateUserId(GlobalContext.getCurrentUser().getId());
            baseDao.save(p);
            //h5
            city.setMessageId(p.getId().toString());
            try {
                p.setH5url(h5url(city));
            } catch (IOException e) {
                // TODO Auto-generated catch block
                e.printStackTrace();
            }
            //添加封面
            if (StringUtils.isNotBlank(city.getMessageImage())) {
                Image i = new Image();
                i.setBelongId(p.getId());
                i.setType("3");
                i.setAddress(city.getMessageImage());
                baseDao.save(i);
            }

            return true;
        }
    }

    @Override
    public Boolean removeDynamics(String[] id) {
        String ids = "";
        for (int i = 0; i < id.length; i++) {
            if (i == id.length - 1) {
                ids += id[i];
            } else {
                ids += id[i] + ",";
            }
        }
        baseDao.executeSql(" update `city_dynamic` set RECORDSTATUS='N' where ID in(" + ids + ")");
        return true;
    }

    @Override
    public Boolean changezDynamics(String[] id) {
        String ids = "";
        for (int i = 0; i < id.length; i++) {
            if (i == id.length - 1) {
                ids += id[i];
            } else {
                ids += id[i] + ",";
            }
        }
        baseDao.executeSql(" UPDATE `city_dynamic` SET IS_ONLINE = CASE IS_ONLINE  WHEN 'Y' THEN 'N' WHEN 'N' THEN 'Y' END WHERE  ID in(" + ids + ")");
        return true;
    }

    @Override
    public DataGridJson guideList(DataGridBean dg, CityBean city) {
        DataGridJson j = new DataGridJson();
        String hql = " from Guide  where recordStatus='Y' ";
        if (StringUtils.isNotBlank(city.getMessageTitle())) {
            hql += " and title like '%%" + city.getMessageTitle() + "%%'";
        }
        if (StringUtils.isNotBlank(city.getMessageIsOnline())) {
            hql += " and isOnline='" + city.getMessageIsOnline() + " '";
        }
        if (StringUtil.isNotBlank(city.getStartTime())) {
            hql += " and createTime>'" + city.getStartTime() + "'";
        }
        if (StringUtil.isNotBlank(city.getEndTime())) {
            hql += " and createTime<'" + city.getEndTime() + "'";
        }
        String totalHql = " select count(*) " + hql;
        j.setTotal(baseDao.count(totalHql));
        if (dg.getOrder() != null) {
            hql += " order by createTime  desc";
        }
        List<Guide> guideList = baseDao.find(hql, dg.getPage(), dg.getRows());
        j.setRows(guideList);
        return j;
    }

    @Override
    public Boolean alterGuide(CityBean city) {
        if (StringUtils.isNotBlank(city.getMessageId())) {//编辑，不支持更改封面图片，其他图片存在html
            Guide p = (Guide) baseDao.get(Guide.class, Long.valueOf(city.getMessageId()));
            p.setTitle(city.getMessageTitle());
            p.setSubtitle(city.getMessageSubTitle());
            p.setContent(city.getMessageContent());
            p.setModifyTime(new Date());
            p.setModifyUserId(GlobalContext.getCurrentUser().getId());
            baseDao.update(p);
            return false;
        } else {//添加
            Guide p = new Guide();
            p.setTitle(city.getMessageTitle());
            p.setSubtitle(city.getMessageSubTitle());
            p.setContent(city.getMessageContent());
            p.setIsOnline("N");
            p.setRecordStatus("Y");
            p.setCreateTime(new Date());
            p.setCreateUserName(GlobalContext.getCurrentUser().getNickName());
            p.setCreateUserId(GlobalContext.getCurrentUser().getId());
            baseDao.save(p);
            return true;
        }
    }

    @Override
    public Boolean removeGuides(String[] id) {
        String ids = "";
        for (int i = 0; i < id.length; i++) {
            if (i == id.length - 1) {
                ids += id[i];
            } else {
                ids += id[i] + ",";
            }
        }
        baseDao.executeSql(" update `city_guide` set RECORDSTATUS='N' where ID in(" + ids + ")");
        return true;
    }

    @Override
    public Boolean changeGuides(String[] id) {
        String ids = "";
        for (int i = 0; i < id.length; i++) {
            if (i == id.length - 1) {
                ids += id[i];
            } else {
                ids += id[i] + ",";
            }
        }
        baseDao.executeSql(" UPDATE `city_guide` SET IS_ONLINE = CASE IS_ONLINE  WHEN 'Y' THEN 'N' WHEN 'N' THEN 'Y' END WHERE  ID in(" + ids + ")");
        return true;
    }

    @Override
    public DataGridJson forumList(DataGridBean dg, CityBean city) {
        DataGridJson j = new DataGridJson();
        String hql = " from Forum  where recordStatus='Y'";
        if (StringUtil.isNotBlank(city.getPhotoName())) {
            hql += " and title like '%%" + city.getPhotoName() + "%%'";
        }
        if (StringUtil.isNotBlank(city.getState())) {
            hql += " and isCheck='" + city.getState() + "'";
        }
        if (StringUtil.isNotBlank(city.getStartTime())) {
            hql += " and createTime>'" + city.getStartTime() + "'";
        }
        if (StringUtil.isNotBlank(city.getEndTime())) {
            hql += " and createTime<'" + city.getEndTime() + "'";
        }
        String totalHql = " select count(*) " + hql;
        j.setTotal(baseDao.count(totalHql));
        if (dg.getOrder() != null) {
            hql += " order by createTime  desc";
        }
        List<Forum> forumList = baseDao.find(hql, dg.getPage(), dg.getRows());
        j.setRows(forumList);
        return j;
    }

    @Override
    public Boolean changeForumState(CityBean city) {
        PartToState p = new PartToState();
        p.setBelongId(Long.valueOf(city.getPhotoId()));
        p.setName(city.getState());
        p.setCreateTime(new Date());
        p.setCreateUserId(GlobalContext.getCurrentUser().getId());
        baseDao.save(p);
        Forum t = (Forum) baseDao.get(Forum.class, Long.valueOf(city.getPhotoId()));
        if (t != null) {
            t.setIsCheck(city.getCode());
            baseDao.update(t);
        }
        return true;
    }

    @Override
    public Boolean removeForums(String[] id) {
        String ids = "";
        for (int i = 0; i < id.length; i++) {
            if (i == id.length - 1) {
                ids += id[i];
            } else {
                ids += id[i] + ",";
            }
        }
        baseDao.executeSql(" update `city_forum` set RECORDSTATUS='N' where ID in(" + ids + ")");
        return true;
    }

    @Override
    public DataGridJson bannerList(DataGridBean dg, CityBean city) {
        DataGridJson j = new DataGridJson();
        String sql = " select cb.`isUrl`,cb.`urlAddress`,cb.`article_type`,cd.`TITLE` title1,img1.`ADDRESS`img1," +
                " cm.`TITLE`title2,img2.`ADDRESS`img2,img3.`ADDRESS`img3,cb.`ORDERNUMBER`,cb.`ID`   from `city_banner` cb " +
                " left join `city_dynamic` cd on cb.`article_id`=cd.`ID` " +
                " left join `city_message` cm on cb.`article_id`=cm.`ID` " +
                " left join `city_part_to_images` img1 on img1.`BELONG_ID`=cd.`ID`" +
                " left join `city_part_to_images` img2 on img2.`BELONG_ID`=cm.`ID` " +
                " left join `city_part_to_images` img3 on img3.`BELONG_ID`=cb.`ID`" +
                " where cb.`recordStatus`='Y'";


        List<Object[]> list = baseDao.findBySql(sql);
        j.setTotal(Long.valueOf(list.size()));
        List data = new ArrayList();
//		List<Position> positionList=baseDao.find(sql, dg.getPage(),dg.getRows());
        for (int i = 0; i < list.size(); i++) {
            Map<String, Object> m = new HashMap<String, Object>();
            if (list.get(i)[0].equals("Y")) {//url链接
                m.put("type", "url外部链接");
                m.put("title", list.get(i)[1]);
                m.put("img", list.get(i)[7]);
                m.put("orderNum", list.get(i)[8]);
                m.put("id", list.get(i)[9]);

            } else {
                if (list.get(i)[2].equals("1")) {//动态
                    m.put("type", "城管动态");
                    m.put("title", list.get(i)[3]);
                    m.put("img", list.get(i)[4]);
                    m.put("orderNum", list.get(i)[8]);
                    m.put("id", list.get(i)[9]);
                } else if (list.get(i)[2].equals("2")) {//资讯
                    m.put("type", "政策资讯");
                    m.put("title", list.get(i)[5]);
                    m.put("img", list.get(i)[6]);
                    m.put("orderNum", list.get(i)[8]);
                    m.put("id", list.get(i)[9]);
                }
            }
            data.add(m);
        }
        j.setRows(data);
        return j;
    }

    @Override
    public Boolean addBanner(CityBean city) {//dynamic
        if ((baseDao.find(" from Banner where recordStatus='Y'")).size() == 4) {
            return false;
        }
        if (city.getCode().equals("1")) {
            Banner b = new Banner();
            b.setIsUrl("N");
            b.setArticleId(Long.valueOf(city.getMessageId()));
            b.setArticleType("1");
            b.setRecordStatus("Y");
            b.setOrderNumber(1);
            baseDao.save(b);

        } else if (city.getCode().equals("2")) {//message

            Banner b = new Banner();
            b.setIsUrl("N");
            b.setArticleId(Long.valueOf(city.getMessageId()));
            b.setArticleType("2");
            b.setRecordStatus("Y");
            b.setOrderNumber(1);
            baseDao.save(b);

        } else if (city.getCode().equals("3")) {//url
            Banner b = new Banner();
            b.setIsUrl("Y");
            b.setUrlAddress(city.getRemark());
            b.setRecordStatus("Y");
            b.setOrderNumber(1);
            baseDao.save(b);

            Image i = new Image();
            i.setBelongId(b.getId());
            i.setType("7");
            i.setAddress(city.getPhotoName());
            baseDao.save(i);
        }
        return true;
    }

    @Override
    public Boolean removeBanners(String[] id) {
        String ids = "";
        for (int i = 0; i < id.length; i++) {
            if (i == id.length - 1) {
                ids += id[i];
            } else {
                ids += id[i] + ",";
            }
        }
        baseDao.executeSql(" update `city_banner` set RECORDSTATUS='N' where ID in(" + ids + ")");
        return true;
    }

    @Override
    public Boolean changeBanners(String[] id) {
        String ids = "";
        for (int i = 0; i < id.length; i++) {
            if (i == id.length - 1) {
                ids += id[i];
            } else {
                ids += id[i] + ",";
            }
        }
        baseDao.executeSql(" UPDATE `city_banner` SET ORDERNUMBER = CASE ORDERNUMBER  WHEN '1' THEN '2' WHEN '2' THEN '3'" +
                " WHEN '3' THEN '4' WHEN '4' THEN '1' END WHERE  ID in(" + ids + ")");
        return true;
    }

    @Override
    public DataGridJson lostList(DataGridBean dg, CityBean city) {
        DataGridJson j = new DataGridJson();
        String hql = " from Lost  where recordStatus='Y'";
        if (StringUtil.isNotBlank(city.getPhotoName())) {
            hql += " and content like '%%" + city.getPhotoName() + "%%'";
        }
        if (StringUtil.isNotBlank(city.getStartTime())) {
            hql += " and createTime>'" + city.getStartTime() + "'";
        }
        if (StringUtil.isNotBlank(city.getEndTime())) {
            hql += " and createTime<'" + city.getEndTime() + "'";
        }
        String totalHql = " select count(*) " + hql;
        j.setTotal(baseDao.count(totalHql));
        if (dg.getOrder() != null) {
            hql += " order by createTime  desc";
        }
        List<Lost> lostList = baseDao.find(hql, dg.getPage(), dg.getRows());
        j.setRows(lostList);
        return j;
    }

    @Override
    public Boolean removeLost(String[] id) {
        String ids = "";
        for (int i = 0; i < id.length; i++) {
            if (i == id.length - 1) {
                ids += id[i];
            } else {
                ids += id[i] + ",";
            }
        }
        baseDao.executeSql(" update `city_lost` set RECORDSTATUS='N' where ID in(" + ids + ")");
        return true;
    }

    @Override
    public Photo photoDetail(String id) {
        Photo p = (Photo) baseDao.get(Photo.class, Long.valueOf(id));
        List l = baseDao.find(" from PartToState where belongId=?  order by createTime desc", p.getId());
        p.setAllState(l);
        return p;
    }

    @Override
    public DataGridJson dynamicCommentList(DataGridBean dg, CityBean city) {
        DataGridJson j = new DataGridJson();
        String hql = " from Comment  where recordStatus='Y' and type='1' ";//动态评论
        if (StringUtil.isNotBlank(city.getMessageId())) {
            hql += " and belongId=" + city.getMessageId();
        }
        String totalHql = " select count(*) " + hql;
        j.setTotal(baseDao.count(totalHql));
        if (dg.getOrder() != null) {
            hql += " order by createTime  desc";
        }
        List<Comment> lostList = baseDao.find(hql, dg.getPage(), dg.getRows());
        List l = new ArrayList();
        for (int i = 0; i < lostList.size(); i++) {
            Map map = new HashMap();
            map.put("id", lostList.get(i).getId());
            map.put("createTime", lostList.get(i).getCreateTime());
            map.put("createUserName", ((User) baseDao.get(User.class, lostList.get(i).getCreateUserId())).getNickName());
            map.put("content", lostList.get(i).getContent());
            l.add(map);
        }
        j.setRows(l);
        return j;
    }

    @Override
    public Boolean removeDynamicComment(String[] id) {
        String ids = "";
        for (int i = 0; i < id.length; i++) {
            if (i == id.length - 1) {
                ids += id[i];
            } else {
                ids += id[i] + ",";
            }
        }
        baseDao.executeSql(" update `city_comment` set RECORDSTATUS='N' where ID in(" + ids + ") and TYPE='1' ");
        return true;
    }

    @Override
    public DataGridJson forumsCommentList(DataGridBean dg, CityBean city) {
        DataGridJson j = new DataGridJson();
        String hql = " from Comment  where recordStatus='Y' and type='2' ";//论坛评论
        if (StringUtil.isNotBlank(city.getMessageId())) {
            hql += " and belongId=" + city.getMessageId();
        }
        String totalHql = " select count(*) " + hql;
        j.setTotal(baseDao.count(totalHql));
        if (dg.getOrder() != null) {
            hql += " order by createTime  desc";
        }
        List<Comment> lostList = baseDao.find(hql, dg.getPage(), dg.getRows());
        List l = new ArrayList();
        for (int i = 0; i < lostList.size(); i++) {
            Map map = new HashMap();
            map.put("id", lostList.get(i).getId());
            map.put("createTime", lostList.get(i).getCreateTime());
            map.put("createUserName", ((User) baseDao.get(User.class, lostList.get(i).getCreateUserId())).getNickName());
            map.put("content", lostList.get(i).getContent());
            l.add(map);
        }
        j.setRows(l);
        return j;
    }

    @Override
    public Boolean removeForumsComment(String[] id) {
        String ids = "";
        for (int i = 0; i < id.length; i++) {
            if (i == id.length - 1) {
                ids += id[i];
            } else {
                ids += id[i] + ",";
            }
        }
        baseDao.executeSql(" update `city_comment` set RECORDSTATUS='N' where ID in(" + ids + ") and TYPE='2'");
        return true;
    }


    //生成h5
    public String h5url(CityBean bean) throws IOException {
        FileInputStream fis = null;
        InputStreamReader isr = null;
        BufferedReader br = null;
        FileOutputStream fos = null;
//        PrintWriter pw = null;
        BufferedWriter pw = null;
        String temp = "";
        String subTitle = bean.getMessageSubTitle() == null ? "" : bean.getMessageSubTitle();

        SimpleDateFormat format = new SimpleDateFormat("yyyyMMdd");
        String fileName = "/data/uploads/dmkAndMsgH5/" + Encrypt.md5(bean.getMessageId()) + System.currentTimeMillis() + ".html";

        String data = " <!DOCTYPE html><html><head><meta http-equiv='content-type' content='text/html;charset=UTF-8'/> "
                + "<title>城管分享</title></head><body><div><p style='font-weight:bold；font-size:14px'>" + bean.getMessageTitle()
                + "</p></br><p style='font-weight:bold；font-size:13px'>" + subTitle + "</p></br>"
                + bean.getMessageContent() + "</div></body></html>";
        try {

            File file = new File(fileName);

            //if file doesnt exists, then create it
            if (!file.exists()) {
                file.createNewFile();
            }


            //将文件读入输入流
            fis = new FileInputStream(file);
            isr = new InputStreamReader(fis, "UTF-8");
            br = new BufferedReader(isr);
            StringBuffer buffer = new StringBuffer();

            //文件原有内容
            for (int i = 0; (temp = br.readLine()) != null; i++) {
                buffer.append(temp);
                // 行与行之间的分隔符 相当于“\n”
                buffer = buffer.append(System.getProperty("line.separator"));
            }
            buffer.append(data);

            fos = new FileOutputStream(file);
//		             pw = new PrintWriter(fos);
            OutputStreamWriter write = new OutputStreamWriter(fos, "UTF-8");
            pw = new BufferedWriter(write);
            pw.write(buffer.toString().toCharArray());
            pw.flush();

        } catch (IOException e) {
            e.printStackTrace();
        } finally {
            //不要忘记关闭
            if (pw != null) {
                pw.close();
            }
            if (fos != null) {
                fos.close();
            }
            if (br != null) {
                br.close();
            }
            if (isr != null) {
                isr.close();
            }
            if (fis != null) {
                fis.close();
            }
        }
        return fileName;
    }

    @Override
    public Boolean removePhotos(String[] id) {
        String ids = "";
        for (int i = 0; i < id.length; i++) {
            if (i == id.length - 1) {
                ids += id[i];
            } else {
                ids += id[i] + ",";
            }
        }
        baseDao.executeSql(" update `city_photo` set RECORDSTATUS='N' where ID in(" + ids + ")");
        return true;
    }

    @Override
    public DataGridJson photoTypeList(DataGridBean dg, String typeName) {
        DataGridJson j = new DataGridJson();
        String hql = " from PhotoType  where recordStatus='Y' ";
        if (StringUtil.isNotBlank(typeName)) {
            hql += " and name like '%" + typeName + "%' ";
        }
        String totalHql = " select count(*) " + hql;
        j.setTotal(baseDao.count(totalHql));
        if (dg != null && dg.getOrder() != null) {
            hql += " order by createTime  desc";
        }
        List<PhotoType> typeList = baseDao.find(hql, dg.getPage(), dg.getRows());
        List l = new ArrayList();
        for (int i = 0; i < typeList.size(); i++) {
            Map map = new HashMap();
            map.put("id", typeList.get(i).getId());
            map.put("createTime", typeList.get(i).getCreateTime());
            map.put("createUserName", ((User) baseDao.get(User.class, typeList.get(i).getCreateUserId())).getNickName());
            map.put("name", typeList.get(i).getName());
            l.add(map);
        }
        j.setRows(l);
        return j;
    }

    @Override
    public Boolean removePhotoType(String[] id) {
        String ids = "";
        for (int i = 0; i < id.length; i++) {
            if (i == id.length - 1) {
                ids += id[i];
            } else {
                ids += id[i] + ",";
            }
        }
        baseDao.executeSql(" update `city_photo_type` set RECORDSTATUS='N' where ID in(" + ids + ")");
        return true;
    }

    @Override
    public Boolean alterPhotoType(PhotoType type) {
        if (type.getId() != null) {
            PhotoType p = (PhotoType) baseDao.get(PhotoType.class, Long.valueOf(type.getId()));
            p.setName(type.getName());
            p.setModifyUserId(GlobalContext.getCurrentUser().getId());
            baseDao.update(p);
            return false;
        } else {
            PhotoType p = new PhotoType();
            p.setName(type.getName());
            p.setRecordStatus("Y");
            p.setCreateTime(new Date());
            p.setCreateUserId(GlobalContext.getCurrentUser().getId());
            p.setCreateUserName(GlobalContext.getCurrentUser().getNickName());
            baseDao.save(p);
            return true;
        }
    }

    @Override
    public String createH5(String id) {

        //随手拍
        Photo p = (Photo) baseDao.get(Photo.class, Long.valueOf(id));

        String createUser;
        if (StringUtil.isNotBlank(p.getCreateUserName().getNickName())) {
            createUser = p.getCreateUserName().getNickName();
        } else {
            createUser = p.getCreateUserName().getMobilePhone();
        }

        //图片标签
        String imgs = "";
        if (p.getImages().size() > 0) {
            for (Image i : p.getImages()) {
                imgs += "<img style='width:400px;height:400px;margin-left:20px' src=" + i.getAddress() + ">";
            }
        }

        FileInputStream fis = null;
        InputStreamReader isr = null;
        BufferedReader br = null;
        FileOutputStream fos = null;
//        PrintWriter pw = null;
        BufferedWriter pw = null;
        String temp = "";


        String user = p.getCreateUserName().getNickName() == null ? p.getCreateUserName().getMobilePhone() : p.getCreateUserName().getNickName();
        String addr = p.getAddrName() == null ? "" : p.getAddrName();
        String content = p.getContent();

        SimpleDateFormat format = new SimpleDateFormat("yyyyMMdd");
        String fileName = "/data/uploads/photo/" + Encrypt.md5(p.getId().toString()) + System.currentTimeMillis() + ".html";

        String data = " <!DOCTYPE html><html><head><meta http-equiv='content-type' content='text/html;charset=UTF-8'/> "
                + "<title>城管分享</title></head><body><div>"
                //+ "<p style='font-weight:bold；font-size:14px'>上传者："+user+"</p></br>"
                + "<p style='font-weight:bold；font-size:13px'>问题描述：" + content + "</p></br>"
                + "<p style='font-weight:bold；font-size:13px'>上传地址：" + addr + "</p></br>"
                + "<p style='font-weight:bold；font-size:13px'>照片详情：</p></br>" + imgs
                + "</div></body></html>";
        try {

            File file = new File(fileName);

            //if file doesnt exists, then create it
            if (!file.exists()) {
                file.createNewFile();
            }


            //将文件读入输入流
            fis = new FileInputStream(file);
            isr = new InputStreamReader(fis, "UTF-8");
            br = new BufferedReader(isr);
            StringBuffer buffer = new StringBuffer();

            //文件原有内容
            for (int i = 0; (temp = br.readLine()) != null; i++) {
                buffer.append(temp);
                // 行与行之间的分隔符 相当于“\n”
                buffer = buffer.append(System.getProperty("line.separator"));
            }
            buffer.append(data);

            fos = new FileOutputStream(file);
//		             pw = new PrintWriter(fos);
            OutputStreamWriter write = new OutputStreamWriter(fos, "UTF-8");
            pw = new BufferedWriter(write);
            pw.write(buffer.toString().toCharArray());
            pw.flush();

        } catch (IOException e) {
            e.printStackTrace();
        } finally {
            try {
                //不要忘记关闭
                if (pw != null) {
                    pw.close();
                }
                if (fos != null) {
                    fos.close();
                }
                if (br != null) {
                    br.close();
                }
                if (isr != null) {
                    isr.close();
                }
                if (fis != null) {
                    fis.close();
                }
            } catch (IOException e) {
                // TODO Auto-generated catch block
                e.printStackTrace();
            }
        }
        p.setH5url(ConfigUtil.get("h5addr") + fileName);
//	      p.setH5url("http://111.231.222.163:8080"+fileName);
//	    	  p.setH5url("http://47.105.53.173:8080"+fileName);
        baseDao.update(p);
        return fileName;
    }

    @Override
    public Boolean removeHandleImage(String id) {
        baseDao.executeSql(" delete from city_handle where id=" + id);
        return true;
    }

    @Override
    public Boolean addHandleImage(CityBean city) {
        //保存处理图片
        if (StringUtil.isNotBlank(city.getMessageImage())) {
            String[] imgs = city.getMessageImage().split(",");
            for (int i = 0; i < imgs.length; i++) {
                Handle h = new Handle();
                h.setBelongId(Long.valueOf(city.getPhotoId()));
                h.setAddress(imgs[i]);
                baseDao.save(h);
            }
        }
        return true;
    }

    @Override
    public void createHistory(String type) {

        List<Object[]> list = new ArrayList();
        //查询数据
        if (type.equals("1")) {
            list = baseDao.findBySql(" select ID,TITLE,SUBTITLE,CONTENT from city_dynamic where h5url is null");
        } else if (type.equals("2")) {
            list = baseDao.findBySql(" select ID,TITLE,SUBTITLE,CONTENT from city_message where h5url is null ");
        }

        for (int i = 0; i < list.size(); i++) {
            CityBean bean = new CityBean();
            bean.setMessageId(list.get(i)[0].toString());
            bean.setMessageTitle(list.get(i)[1].toString());
            bean.setMessageSubTitle(list.get(i)[2].toString());
            bean.setMessageContent(list.get(i)[3].toString());

            try {
                if (type.equals("1")) {
                    Dynamic d = (Dynamic) baseDao.get(Dynamic.class, Long.valueOf(bean.getMessageId()));
                    d.setH5url(h5url(bean));
                    baseDao.update(d);
                } else if (type.equals("2")) {
                    Message d = (Message) baseDao.get(Message.class, Long.valueOf(bean.getMessageId()));
                    d.setH5url(h5url(bean));
                    baseDao.update(d);
                }

            } catch (IOException e) {
                // TODO Auto-generated catch block
                e.printStackTrace();
            }
        }

    }

}

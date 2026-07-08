package com.lsp.infrastructure.dao;

import com.lsp.infrastructure.dao.po.GroupBuyProgress;
import org.apache.ibatis.annotations.Mapper;
import org.apache.ibatis.annotations.Param;

import java.util.List;
import java.util.Set;

@Mapper
public interface IGroupBuyProgressDao {

    List<GroupBuyProgress> queryGroupBuyProgressByTeamIds(@Param("teamIds") Set<String> teamIds);

}

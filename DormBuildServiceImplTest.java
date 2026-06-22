package com.example.springboot.service.impl;

import com.baomidou.mybatisplus.core.conditions.query.QueryWrapper;
import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import com.example.springboot.entity.DormBuild;
import com.example.springboot.mapper.DormBuildMapper;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import org.springframework.test.util.ReflectionTestUtils;

import java.util.Arrays;
import java.util.Collections;
import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

/**
 * 楼宇管理模块 — Service 层单元测试
 * 覆盖 addNewBuilding / find / updateNewBuilding / deleteBuilding / getBuildingId
 */
@DisplayName("楼宇管理 Service 层测试")
class DormBuildServiceImplTest {

    /* ==================== 添加楼宇 ==================== */

    @Nested
    @DisplayName("添加楼宇 — addNewBuilding")
    class AddNewBuilding {

        @Test
        @DisplayName("添加成功：有效的楼宇信息应返回 1")
        void shouldReturnOneWhenInsertSucceeds() {
            DormBuildImpl service = newService();
            DormBuildMapper mapper = mapper(service);

            DormBuild building = new DormBuild(null, 6, "6号楼", "新建留学生宿舍楼");
            when(mapper.insert(building)).thenReturn(1);

            int result = service.addNewBuilding(building);

            assertThat(result).isEqualTo(1);
            verify(mapper).insert(building);
        }

        @Test
        @DisplayName("添加失败：数据库插入返回 0 时应返回 0")
        void shouldReturnZeroWhenInsertFails() {
            DormBuildImpl service = newService();
            DormBuildMapper mapper = mapper(service);

            DormBuild building = new DormBuild(null, 6, "6号楼", "新建留学生宿舍楼");
            when(mapper.insert(building)).thenReturn(0);

            int result = service.addNewBuilding(building);

            assertThat(result).isEqualTo(0);
        }

        @Test
        @DisplayName("添加：传入的 dormBuildId 应正确持久化")
        void shouldPersistCorrectDormBuildId() {
            DormBuildImpl service = newService();
            DormBuildMapper mapper = mapper(service);

            DormBuild building = new DormBuild(null, 99, "99号楼", "远端宿舍楼");
            when(mapper.insert(building)).thenReturn(1);

            service.addNewBuilding(building);

            verify(mapper).insert(org.mockito.ArgumentMatchers.argThat(b ->
                    b.getDormBuildId() == 99
                            && "99号楼".equals(b.getDormBuildName())
                            && "远端宿舍楼".equals(b.getDormBuildDetail())));
        }
    }

    /* ==================== 分页查询楼宇 ==================== */

    @Nested
    @DisplayName("分页查询楼宇 — find")
    class Find {

        @Test
        @DisplayName("无搜索关键字：返回全部楼宇分页数据")
        void shouldReturnAllBuildingsWhenSearchIsEmpty() {
            DormBuildImpl service = newService();
            DormBuildMapper mapper = mapper(service);

            Page<DormBuild> mockPage = new Page<>(1, 10);
            mockPage.setRecords(Arrays.asList(
                    new DormBuild(1, 1, "1号楼", "男生宿舍楼"),
                    new DormBuild(2, 2, "2号楼", "女生宿舍楼")
            ));
            mockPage.setTotal(2);
            when(mapper.selectPage(any(Page.class), any(QueryWrapper.class))).thenReturn(mockPage);

            Page result = service.find(1, 10, "");

            assertThat(result).isNotNull();
            assertThat(result.getRecords()).hasSize(2);
            assertThat(result.getTotal()).isEqualTo(2);
        }

        @Test
        @DisplayName("按楼宇编号搜索：应返回匹配的楼宇")
        void shouldFilterByDormBuildIdWhenSearchKeywordProvided() {
            DormBuildImpl service = newService();
            DormBuildMapper mapper = mapper(service);

            Page<DormBuild> mockPage = new Page<>(1, 10);
            mockPage.setRecords(Collections.singletonList(
                    new DormBuild(3, 3, "3号楼", "综合宿舍楼")
            ));
            mockPage.setTotal(1);
            when(mapper.selectPage(any(Page.class), any(QueryWrapper.class))).thenReturn(mockPage);

            Page result = service.find(1, 10, "3");

            assertThat(result).isNotNull();
            assertThat(result.getRecords()).hasSize(1);
            assertThat(((DormBuild) result.getRecords().get(0)).getDormBuildId()).isEqualTo(3);
        }

        @Test
        @DisplayName("搜索不存在的楼宇：应返回空列表")
        void shouldReturnEmptyWhenNoMatch() {
            DormBuildImpl service = newService();
            DormBuildMapper mapper = mapper(service);

            Page<DormBuild> mockPage = new Page<>(1, 10);
            mockPage.setRecords(Collections.emptyList());
            mockPage.setTotal(0);
            when(mapper.selectPage(any(Page.class), any(QueryWrapper.class))).thenReturn(mockPage);

            Page result = service.find(1, 10, "999");

            assertThat(result).isNotNull();
            assertThat(result.getRecords()).isEmpty();
            assertThat(result.getTotal()).isEqualTo(0);
        }

        @Test
        @DisplayName("分页参数：pageNum=2, pageSize=5 应正确传递")
        void shouldPassCorrectPaginationParams() {
            DormBuildImpl service = newService();
            DormBuildMapper mapper = mapper(service);

            Page<DormBuild> mockPage = new Page<>(2, 5);
            mockPage.setRecords(Collections.emptyList());
            mockPage.setTotal(0);
            when(mapper.selectPage(any(Page.class), any(QueryWrapper.class))).thenReturn(mockPage);

            Page result = service.find(2, 5, "");

            assertThat(result).isNotNull();
            assertThat(result.getCurrent()).isEqualTo(2);
            assertThat(result.getSize()).isEqualTo(5);
        }
    }

    /* ==================== 更新楼宇 ==================== */

    @Nested
    @DisplayName("更新楼宇 — updateNewBuilding")
    class UpdateNewBuilding {

        @Test
        @DisplayName("更新成功：存在的楼宇应更新并返回 1")
        void shouldReturnOneWhenUpdateSucceeds() {
            DormBuildImpl service = newService();
            DormBuildMapper mapper = mapper(service);

            DormBuild building = new DormBuild(1, 1, "1号楼（已翻新）", "男生宿舍楼，2024年翻新");
            when(mapper.updateById(building)).thenReturn(1);

            int result = service.updateNewBuilding(building);

            assertThat(result).isEqualTo(1);
            verify(mapper).updateById(building);
        }

        @Test
        @DisplayName("更新失败：不存在的楼宇应返回 0")
        void shouldReturnZeroWhenBuildingNotFound() {
            DormBuildImpl service = newService();
            DormBuildMapper mapper = mapper(service);

            DormBuild building = new DormBuild(9999, 9999, "不存在的楼", "不存在");
            when(mapper.updateById(building)).thenReturn(0);

            int result = service.updateNewBuilding(building);

            assertThat(result).isEqualTo(0);
        }

        @Test
        @DisplayName("更新：仅修改备注字段时名称应保持不变")
        void shouldOnlyUpdateProvidedFields() {
            DormBuildImpl service = newService();
            DormBuildMapper mapper = mapper(service);

            DormBuild building = new DormBuild(1, 1, "1号楼", "仅修改了备注信息");
            when(mapper.updateById(building)).thenReturn(1);

            service.updateNewBuilding(building);

            verify(mapper).updateById(org.mockito.ArgumentMatchers.argThat(b ->
                    b.getId() == 1
                            && b.getDormBuildId() == 1
                            && "1号楼".equals(b.getDormBuildName())
                            && "仅修改了备注信息".equals(b.getDormBuildDetail())));
        }
    }

    /* ==================== 删除楼宇 ==================== */

    @Nested
    @DisplayName("删除楼宇 — deleteBuilding")
    class DeleteBuilding {

        @Test
        @DisplayName("删除成功：存在的楼宇 ID 应删除并返回 1")
        void shouldReturnOneWhenDeleteSucceeds() {
            DormBuildImpl service = newService();
            DormBuildMapper mapper = mapper(service);

            when(mapper.deleteById(5)).thenReturn(1);

            int result = service.deleteBuilding(5);

            assertThat(result).isEqualTo(1);
            verify(mapper).deleteById(5);
        }

        @Test
        @DisplayName("删除失败：不存在的楼宇 ID 应返回 0")
        void shouldReturnZeroWhenBuildingNotFound() {
            DormBuildImpl service = newService();
            DormBuildMapper mapper = mapper(service);

            when(mapper.deleteById(9999)).thenReturn(0);

            int result = service.deleteBuilding(9999);

            assertThat(result).isEqualTo(0);
        }
    }

    /* ==================== 获取楼宇 ID 列表（首页图表用） ==================== */

    @Nested
    @DisplayName("获取楼宇 ID 列表 — getBuildingId")
    class GetBuildingId {

        @Test
        @DisplayName("有楼宇数据：应返回 dormbuild_id 列表")
        void shouldReturnBuildingIdList() {
            DormBuildImpl service = newService();
            DormBuildMapper mapper = mapper(service);

            List<DormBuild> mockList = Arrays.asList(
                    new DormBuild(null, 1, null, null),
                    new DormBuild(null, 2, null, null),
                    new DormBuild(null, 3, null, null)
            );
            when(mapper.selectList(any(QueryWrapper.class))).thenReturn(mockList);

            List<DormBuild> result = service.getBuildingId();

            assertThat(result).hasSize(3);
            assertThat(result).extracting("dormBuildId").containsExactly(1, 2, 3);
        }

        @Test
        @DisplayName("无楼宇数据：应返回空列表")
        void shouldReturnEmptyListWhenNoBuildings() {
            DormBuildImpl service = newService();
            DormBuildMapper mapper = mapper(service);

            when(mapper.selectList(any(QueryWrapper.class))).thenReturn(Collections.emptyList());

            List<DormBuild> result = service.getBuildingId();

            assertThat(result).isEmpty();
        }

        @Test
        @DisplayName("查询语句应仅选择 dormbuild_id 列")
        void shouldOnlySelectDormBuildIdColumn() {
            DormBuildImpl service = newService();
            DormBuildMapper mapper = mapper(service);

            when(mapper.selectList(any(QueryWrapper.class))).thenReturn(Collections.emptyList());

            service.getBuildingId();

            verify(mapper).selectList(org.mockito.ArgumentMatchers.argThat(qw ->
                    qw.getSqlSelect() != null && qw.getSqlSelect().contains("dormbuild_id")));
        }
    }

    /* ==================== 辅助方法 ==================== */

    private DormBuildImpl newService() {
        DormBuildImpl service = new DormBuildImpl();
        ReflectionTestUtils.setField(service, "dormBuildMapper", mock(DormBuildMapper.class));
        return service;
    }

    @SuppressWarnings("unchecked")
    private DormBuildMapper mapper(Object service) {
        return (DormBuildMapper) ReflectionTestUtils.getField(service, "dormBuildMapper");
    }
}

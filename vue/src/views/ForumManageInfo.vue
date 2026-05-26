<template>
  <div>
    <el-breadcrumb separator-icon="ArrowRight" style="margin: 16px">
      <el-breadcrumb-item :to="{ path: '/home' }">首页</el-breadcrumb-item>
      <el-breadcrumb-item>信息管理</el-breadcrumb-item>
      <el-breadcrumb-item>帖子管理</el-breadcrumb-item>
    </el-breadcrumb>
    <el-card style="margin: 15px; min-height: calc(100vh - 111px)">
      <div class="toolbar">
        <el-select v-model="category" clearable placeholder="分类" style="width: 140px" @change="load">
          <el-option v-for="item in categories" :key="item" :label="item" :value="item"/>
        </el-select>
        <el-input v-model="search" clearable placeholder="搜索标题或内容" prefix-icon="Search" style="width: 260px"/>
        <el-button icon="Search" type="primary" @click="load"></el-button>
        <el-button icon="RefreshLeft" @click="reset"></el-button>
      </div>

      <el-table v-loading="loading" :data="tableData" border max-height="690" style="width: 100%">
        <el-table-column label="#" type="index" width="60"/>
        <el-table-column label="标题" min-width="220" prop="title" show-overflow-tooltip/>
        <el-table-column label="分类" prop="category" width="100"/>
        <el-table-column label="作者" width="180">
          <template #default="scope">
            {{ scope.row.authorName }} · {{ scope.row.authorUsername }}
          </template>
        </el-table-column>
        <el-table-column label="发布时间" prop="createTime" sortable width="180"/>
        <el-table-column label="操作" width="150">
          <template #default="scope">
            <el-button icon="MoreFilled" type="default" @click="showDetail(scope.row.id)"></el-button>
            <el-popconfirm title="确认删除该帖子？" @confirm="deletePost(scope.row.id)">
              <template #reference>
                <el-button icon="Delete" type="danger"></el-button>
              </template>
            </el-popconfirm>
          </template>
        </el-table-column>
      </el-table>

      <div style="margin: 10px 0">
        <el-pagination
            v-model:currentPage="currentPage"
            :page-size="pageSize"
            :page-sizes="[10, 20]"
            :total="total"
            layout="total, sizes, prev, pager, next, jumper"
            @size-change="handleSizeChange"
            @current-change="handleCurrentChange"
        />
      </div>
    </el-card>

    <el-dialog v-model="detailVisible" title="帖子详情" width="720px">
      <div v-if="detail.post" class="detail">
        <div class="meta">
          <el-tag effect="light">{{ detail.post.category }}</el-tag>
          <span>{{ detail.post.authorName }} · {{ detail.post.authorUsername }}</span>
          <span>{{ detail.post.createTime }}</span>
        </div>
        <h2>{{ detail.post.title }}</h2>
        <p class="content">{{ detail.post.content }}</p>
        <img v-if="detail.post.imageData" :src="'data:image;base64,' + detail.post.imageData" alt="" class="detail-image"/>
        <div class="comments">
          <h3>评论</h3>
          <el-empty v-if="detail.comments.length === 0" description="暂无评论"/>
          <div v-for="comment in detail.comments" :key="comment.id" class="comment">
            <div>
              <strong>{{ comment.authorName }} · {{ comment.authorUsername }}</strong>
              <span>{{ comment.createTime }}</span>
              <p>{{ comment.content }}</p>
            </div>
            <el-popconfirm title="确认删除该评论？" @confirm="deleteComment(comment.id)">
              <template #reference>
                <el-button icon="Delete" text type="danger"></el-button>
              </template>
            </el-popconfirm>
          </div>
        </div>
      </div>
    </el-dialog>
  </div>
</template>

<script>
import request from "@/utils/request";
import {ElMessage} from "element-plus";

export default {
  name: "ForumManageInfo",
  data() {
    return {
      categories: ["失物", "招领", "分享", "求助"],
      category: "",
      search: "",
      loading: false,
      currentPage: 1,
      pageSize: 10,
      total: 0,
      tableData: [],
      detailVisible: false,
      detail: {post: null, comments: []},
    };
  },
  created() {
    this.load();
  },
  methods: {
    load() {
      this.loading = true;
      request.get("/forum/post/find", {
        params: {pageNum: this.currentPage, pageSize: this.pageSize, search: this.search, category: this.category}
      }).then((res) => {
        if (res.code === "0") {
          this.tableData = res.data.records;
          this.total = res.data.total;
        } else {
          ElMessage({message: res.msg, type: "error"});
        }
        this.loading = false;
      });
    },
    reset() {
      this.search = "";
      this.category = "";
      this.currentPage = 1;
      this.load();
    },
    handleSizeChange(pageSize) {
      this.pageSize = pageSize;
      this.load();
    },
    handleCurrentChange(pageNum) {
      this.currentPage = pageNum;
      this.load();
    },
    showDetail(id) {
      request.get("/forum/post/" + id).then(async (res) => {
        if (res.code === "0") {
          this.detail = res.data;
          this.detail.post.imageData = "";
          if (this.detail.post.image) {
            const imgRes = await request.get("/files/image/" + this.detail.post.image);
            if (imgRes.code === "0") this.detail.post.imageData = imgRes.data;
          }
          this.detailVisible = true;
        } else {
          ElMessage({message: res.msg, type: "error"});
        }
      });
    },
    deletePost(id) {
      request.delete("/forum/post/delete/" + id).then((res) => {
        if (res.code === "0") {
          ElMessage({message: "删除成功", type: "success"});
          this.detailVisible = false;
          this.load();
        } else {
          ElMessage({message: res.msg, type: "error"});
        }
      });
    },
    deleteComment(id) {
      request.delete("/forum/comment/delete/" + id).then((res) => {
        if (res.code === "0") {
          this.showDetail(this.detail.post.id);
        } else {
          ElMessage({message: res.msg, type: "error"});
        }
      });
    },
  },
};
</script>

<style scoped>
.toolbar {
  display: flex;
  gap: 8px;
  margin: 10px 0 16px;
}

.meta {
  display: flex;
  align-items: center;
  flex-wrap: wrap;
  gap: 8px;
  color: var(--text-muted);
  font-size: 12px;
}

.detail h2 {
  margin: 12px 0;
  color: var(--text-title);
}

.content,
.comment p {
  white-space: pre-wrap;
  word-break: break-word;
}

.detail-image {
  max-width: 100%;
  max-height: 360px;
  margin-top: 12px;
  border-radius: 8px;
  object-fit: contain;
}

.comments {
  margin-top: 22px;
  padding-top: 18px;
  border-top: 1px solid var(--border-light);
}

.comment {
  display: flex;
  justify-content: space-between;
  gap: 12px;
  padding: 12px 0;
  border-bottom: 1px solid var(--border-light);
}

.comment span {
  margin-left: 8px;
  color: var(--text-muted);
  font-size: 12px;
}
</style>

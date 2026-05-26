<template>
  <div class="forum-page">
    <div class="forum-header">
      <div>
        <p class="eyebrow">校园社区</p>
        <h2>互助与分享</h2>
      </div>
      <el-button icon="Plus" type="primary" @click="openCreate">发布帖子</el-button>
    </div>

    <div class="forum-toolbar">
      <el-radio-group v-model="category" @change="load">
        <el-radio-button label="">全部</el-radio-button>
        <el-radio-button v-for="item in categories" :key="item" :label="item">{{ item }}</el-radio-button>
      </el-radio-group>
      <div class="search-box">
        <el-input v-model="search" clearable placeholder="搜索标题或内容" prefix-icon="Search" @keyup.enter="load"/>
        <el-button icon="Search" type="primary" @click="load"></el-button>
      </div>
    </div>

    <div v-loading="loading" class="post-list">
      <el-empty v-if="!loading && tableData.length === 0" description="暂无帖子"/>
      <article v-for="post in tableData" :key="post.id" class="post-card" @click="showDetail(post.id)">
        <div class="post-main">
          <div class="post-meta">
            <el-tag effect="light">{{ post.category }}</el-tag>
            <span>{{ post.authorName }} · {{ post.authorUsername }}</span>
            <span>{{ post.createTime }}</span>
          </div>
          <h3>{{ post.title }}</h3>
          <p>{{ post.content }}</p>
          <div class="post-actions" @click.stop>
            <el-button icon="ChatLineRound" text type="primary" @click="showDetail(post.id)">查看评论</el-button>
            <el-button v-if="canEdit(post)" icon="Edit" text type="primary" @click="openEdit(post)">编辑</el-button>
            <el-popconfirm v-if="canEdit(post)" title="确认删除该帖子？" @confirm="deletePost(post.id)">
              <template #reference>
                <el-button icon="Delete" text type="danger">删除</el-button>
              </template>
            </el-popconfirm>
          </div>
        </div>
        <img v-if="post.imageData" :src="'data:image;base64,' + post.imageData" alt="" class="post-image"/>
      </article>
    </div>

    <el-pagination
        v-model:currentPage="currentPage"
        :page-size="pageSize"
        :total="total"
        layout="total, prev, pager, next"
        @current-change="load"
    />

    <el-dialog v-model="formVisible" :title="form.id ? '编辑帖子' : '发布帖子'" width="560px" @close="resetForm">
      <el-form ref="formRef" :model="form" :rules="rules" label-width="84px">
        <el-form-item label="分类" prop="category">
          <el-select v-model="form.category" placeholder="请选择分类" style="width: 100%">
            <el-option v-for="item in categories" :key="item" :label="item" :value="item"/>
          </el-select>
        </el-form-item>
        <el-form-item label="标题" prop="title">
          <el-input v-model="form.title" maxlength="80" placeholder="请输入标题" show-word-limit/>
        </el-form-item>
        <el-form-item label="内容" prop="content">
          <el-input v-model="form.content" :autosize="{ minRows: 5, maxRows: 9 }" placeholder="写下你想发布的内容" type="textarea"/>
        </el-form-item>
        <el-form-item label="图片">
          <el-upload
              :on-success="uploadSuccess"
              :show-file-list="false"
              action="/api/files/upload"
              accept="image/*"
          >
            <el-button icon="Upload">上传图片</el-button>
          </el-upload>
          <div v-if="form.imageData" class="upload-preview">
            <img :src="'data:image;base64,' + form.imageData" alt=""/>
            <el-button icon="Close" text type="danger" @click="removeImage">移除</el-button>
          </div>
        </el-form-item>
      </el-form>
      <template #footer>
        <el-button @click="formVisible = false">取消</el-button>
        <el-button type="primary" @click="savePost">保存</el-button>
      </template>
    </el-dialog>

    <el-dialog v-model="detailVisible" title="帖子详情" width="720px">
      <div v-if="detail.post" class="detail">
        <div class="post-meta">
          <el-tag effect="light">{{ detail.post.category }}</el-tag>
          <span>{{ detail.post.authorName }} · {{ detail.post.authorUsername }}</span>
          <span>{{ detail.post.createTime }}</span>
        </div>
        <h2>{{ detail.post.title }}</h2>
        <p class="detail-content">{{ detail.post.content }}</p>
        <img v-if="detail.post.imageData" :src="'data:image;base64,' + detail.post.imageData" alt="" class="detail-image"/>
        <div class="comment-box">
          <h3>评论</h3>
          <div v-for="comment in detail.comments" :key="comment.id" class="comment-item">
            <div>
              <strong>{{ comment.authorName }} · {{ comment.authorUsername }}</strong>
              <span>{{ comment.createTime }}</span>
              <p>{{ comment.content }}</p>
            </div>
            <div v-if="canDeleteComment(comment)" class="comment-tools">
              <el-button icon="Edit" text type="primary" @click="openCommentEdit(comment)"></el-button>
              <el-popconfirm title="确认删除该评论？" @confirm="deleteComment(comment.id)">
                <template #reference>
                  <el-button icon="Delete" text type="danger"></el-button>
                </template>
              </el-popconfirm>
            </div>
          </div>
          <el-input v-model="commentContent" :autosize="{ minRows: 2, maxRows: 4 }" placeholder="写下评论" type="textarea"/>
          <div class="comment-actions">
            <el-button type="primary" @click="addComment">发表评论</el-button>
          </div>
        </div>
      </div>
    </el-dialog>

    <el-dialog v-model="commentEditVisible" title="编辑评论" width="460px">
      <el-input v-model="commentEdit.content" :autosize="{ minRows: 3, maxRows: 5 }" placeholder="请输入评论内容" type="textarea"/>
      <template #footer>
        <el-button @click="commentEditVisible = false">取消</el-button>
        <el-button type="primary" @click="updateComment">保存</el-button>
      </template>
    </el-dialog>
  </div>
</template>

<script>
import request from "@/utils/request";
import {ElMessage} from "element-plus";

export default {
  name: "ForumInfo",
  data() {
    return {
      categories: ["失物", "招领", "分享", "求助"],
      category: "",
      search: "",
      loading: false,
      currentPage: 1,
      pageSize: 8,
      total: 0,
      tableData: [],
      formVisible: false,
      detailVisible: false,
      commentEditVisible: false,
      commentContent: "",
      commentEdit: {id: "", content: ""},
      user: {},
      form: this.emptyForm(),
      detail: {post: null, comments: []},
      rules: {
        category: [{required: true, message: "请选择分类", trigger: "change"}],
        title: [{required: true, message: "请输入标题", trigger: "blur"}],
        content: [{required: true, message: "请输入内容", trigger: "blur"}],
      },
    };
  },
  created() {
    this.user = JSON.parse(sessionStorage.getItem("user") || "{}");
    this.load();
  },
  methods: {
    emptyForm() {
      return {id: "", title: "", content: "", category: "失物", image: "", imageData: ""};
    },
    load() {
      this.loading = true;
      request.get("/forum/post/find", {
        params: {pageNum: this.currentPage, pageSize: this.pageSize, search: this.search, category: this.category}
      }).then(async (res) => {
        if (res.code === "0") {
          this.tableData = res.data.records;
          this.total = res.data.total;
          await this.loadImages(this.tableData);
        } else {
          ElMessage({message: res.msg, type: "error"});
        }
        this.loading = false;
      });
    },
    async loadImages(posts) {
      for (const post of posts) {
        post.imageData = "";
        if (post.image) {
          const res = await request.get("/files/image/" + post.image);
          if (res.code === "0") post.imageData = res.data;
        }
      }
    },
    canEdit(post) {
      return post.authorUsername === this.user.username;
    },
    canDeleteComment(comment) {
      return comment.authorUsername === this.user.username;
    },
    openCreate() {
      this.form = this.emptyForm();
      this.formVisible = true;
    },
    openEdit(post) {
      this.form = JSON.parse(JSON.stringify(post));
      this.formVisible = true;
    },
    uploadSuccess(res) {
      if (res.code === "0") {
        this.form.image = res.data;
        request.get("/files/image/" + res.data).then((imgRes) => {
          if (imgRes.code === "0") this.form.imageData = imgRes.data;
        });
      } else {
        ElMessage({message: res.msg, type: "error"});
      }
    },
    removeImage() {
      this.form.image = "";
      this.form.imageData = "";
    },
    savePost() {
      this.$refs.formRef.validate((valid) => {
        if (!valid) return;
        const payload = JSON.parse(JSON.stringify(this.form));
        delete payload.imageData;
        const req = payload.id ? request.put("/forum/post/update", payload) : request.post("/forum/post/add", payload);
        req.then((res) => {
          if (res.code === "0") {
            ElMessage({message: "保存成功", type: "success"});
            this.formVisible = false;
            this.load();
          } else {
            ElMessage({message: res.msg, type: "error"});
          }
        });
      });
    },
    resetForm() {
      this.form = this.emptyForm();
      this.$nextTick(() => this.$refs.formRef && this.$refs.formRef.clearValidate());
    },
    deletePost(id) {
      request.delete("/forum/post/delete/" + id).then((res) => {
        if (res.code === "0") {
          ElMessage({message: "删除成功", type: "success"});
          this.load();
        } else {
          ElMessage({message: res.msg, type: "error"});
        }
      });
    },
    showDetail(id) {
      request.get("/forum/post/" + id).then(async (res) => {
        if (res.code === "0") {
          this.detail = res.data;
          await this.loadImages([this.detail.post]);
          this.commentContent = "";
          this.detailVisible = true;
        } else {
          ElMessage({message: res.msg, type: "error"});
        }
      });
    },
    addComment() {
      if (!this.commentContent.trim()) {
        ElMessage({message: "请输入评论内容", type: "warning"});
        return;
      }
      request.post("/forum/comment/add", {postId: this.detail.post.id, content: this.commentContent}).then((res) => {
        if (res.code === "0") {
          this.showDetail(this.detail.post.id);
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
    openCommentEdit(comment) {
      this.commentEdit = {id: comment.id, content: comment.content};
      this.commentEditVisible = true;
    },
    updateComment() {
      if (!this.commentEdit.content.trim()) {
        ElMessage({message: "请输入评论内容", type: "warning"});
        return;
      }
      request.put("/forum/comment/update", this.commentEdit).then((res) => {
        if (res.code === "0") {
          this.commentEditVisible = false;
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
.forum-page {
  padding: 20px;
  min-height: 100%;
  overflow-y: auto;
}

.forum-header,
.forum-toolbar,
.post-card,
.post-meta,
.post-actions,
.comment-item {
  display: flex;
}

.forum-header {
  align-items: center;
  justify-content: space-between;
  margin-bottom: 18px;
}

.eyebrow {
  margin: 0 0 4px;
  color: var(--text-muted);
  font-size: 12px;
}

.forum-header h2 {
  margin: 0;
  color: var(--text-title);
  font-size: 26px;
}

.forum-toolbar {
  align-items: center;
  justify-content: space-between;
  gap: 16px;
  margin-bottom: 16px;
}

.search-box {
  display: flex;
  gap: 8px;
  width: 360px;
}

.post-list {
  display: grid;
  gap: 14px;
  min-height: 320px;
}

.post-card {
  align-items: stretch;
  justify-content: space-between;
  gap: 18px;
  padding: 18px;
  border: 1px solid var(--border-light);
  border-radius: 8px;
  background: rgba(255, 255, 255, .94);
  box-shadow: var(--shadow-card);
  cursor: pointer;
}

.post-main {
  min-width: 0;
  flex: 1;
}

.post-meta {
  align-items: center;
  flex-wrap: wrap;
  gap: 8px;
  color: var(--text-muted);
  font-size: 12px;
}

.post-card h3 {
  margin: 10px 0 6px;
  color: var(--text-title);
  font-size: 18px;
}

.post-card p,
.detail-content {
  white-space: pre-wrap;
  word-break: break-word;
}

.post-card p {
  margin: 0;
  color: var(--text-body);
}

.post-actions {
  align-items: center;
  gap: 4px;
  margin-top: 12px;
}

.post-image {
  width: 160px;
  height: 116px;
  border-radius: 8px;
  object-fit: cover;
}

.upload-preview {
  display: flex;
  align-items: center;
  gap: 10px;
  margin-top: 10px;
}

.upload-preview img {
  width: 120px;
  height: 86px;
  border-radius: 8px;
  object-fit: cover;
}

.detail h2 {
  margin: 12px 0;
  color: var(--text-title);
}

.detail-image {
  max-width: 100%;
  max-height: 360px;
  margin-top: 12px;
  border-radius: 8px;
  object-fit: contain;
}

.comment-box {
  margin-top: 22px;
  padding-top: 18px;
  border-top: 1px solid var(--border-light);
}

.comment-box h3 {
  margin-bottom: 12px;
}

.comment-item {
  justify-content: space-between;
  gap: 12px;
  padding: 12px 0;
  border-bottom: 1px solid var(--border-light);
}

.comment-item span {
  margin-left: 8px;
  color: var(--text-muted);
  font-size: 12px;
}

.comment-item p {
  margin-top: 6px;
  white-space: pre-wrap;
}

.comment-actions {
  display: flex;
  justify-content: flex-end;
  margin-top: 10px;
}

.comment-tools {
  display: flex;
  align-items: center;
}

@media (max-width: 760px) {
  .forum-toolbar,
  .forum-header,
  .post-card {
    align-items: stretch;
    flex-direction: column;
  }

  .search-box {
    width: 100%;
  }

  .post-image {
    width: 100%;
    height: 180px;
  }
}
</style>

<template>
  <div class="about-page">
    <header class="page-header">
      <button class="icon-back" type="button" aria-label="返回" @click="goBack">
        <span></span>
      </button>
      <div>
        <p class="eyebrow">关于</p>
        <h1>关于 Ecru</h1>
      </div>
    </header>

    <section class="hero-card">
      <p class="hero-caption">原木、留白、轻一点的穿搭助手。</p>
      <h2>Ecru 想把衣橱、灵感、问答和面料知识，收进同一个干净界面里。</h2>
    </section>

    <section class="content-card">
      <article class="about-block">
        <h3>我们在做什么</h3>
        <p>帮助用户建立个人衣橱，保存喜欢的灵感，结合 AI 对话、知识库与识别能力，给出更贴近日常的穿搭建议。</p>
      </article>

      <article class="about-block">
        <h3>当前支持</h3>
        <ul>
          <li>上传个人衣物，形成可检索的专属衣橱。</li>
          <li>通过首页灵感流收藏喜欢的搭配方向。</li>
          <li>在对话中获取穿搭建议、单品推荐与面料问答。</li>
          <li>查看面料百科和洗护标签相关知识。</li>
        </ul>
      </article>

      <article class="about-block">
        <h3>产品方向</h3>
        <p>我们更在意安静、自然、不打扰的使用感，而不是把页面做成工具台。信息够用，动作够少，风格尽量克制。</p>
      </article>
    </section>
  </div>
</template>

<script setup>
import { useRouter } from 'vue-router';

const router = useRouter();

const goBack = () => {
  if (window.history.length > 1) {
    router.back();
    return;
  }

  router.push('/profile');
};
</script>

<style scoped>
.about-page {
  min-height: 100vh;
  padding: 20px 16px 40px;
  background:
    radial-gradient(circle at top, rgba(255, 252, 246, 0.9), transparent 28%),
    linear-gradient(180deg, var(--bg-base) 0%, var(--bg-soft) 100%);
}

.page-header {
  display: flex;
  align-items: center;
  gap: 14px;
  margin-bottom: 18px;
}

.icon-back {
  display: inline-grid;
  place-items: center;
  width: 34px;
  height: 34px;
  border: 1px solid var(--line-soft);
  border-radius: 50%;
  background: color-mix(in srgb, var(--surface-strong) 90%, transparent);
  cursor: pointer;
}

.icon-back span {
  width: 10px;
  height: 10px;
  border-left: 1.5px solid var(--text-main);
  border-bottom: 1.5px solid var(--text-main);
  transform: rotate(45deg);
  margin-left: 4px;
}

.eyebrow,
.hero-caption {
  color: var(--text-faint);
  font-size: 12px;
  letter-spacing: 0.12em;
}

.hero-card,
.content-card {
  border-radius: 28px;
  border: 1px solid var(--line-soft);
  background: color-mix(in srgb, var(--surface-strong) 92%, transparent);
  box-shadow: var(--shadow-card);
}

.hero-card {
  padding: 20px 18px;
}

.hero-card h2 {
  margin-top: 10px;
  font-size: 26px;
  line-height: 1.45;
}

.content-card {
  margin-top: 16px;
  padding: 18px;
}

.about-block + .about-block {
  margin-top: 22px;
}

.about-block h3 {
  font-size: 18px;
}

.about-block p,
.about-block li {
  margin-top: 10px;
  color: var(--text-soft);
  line-height: 1.8;
}

.about-block ul {
  padding-left: 18px;
}

@media (min-width: 768px) {
  .about-page {
    padding: 28px 24px 40px;
  }
}
</style>

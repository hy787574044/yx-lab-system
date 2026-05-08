<template>
  <div class="content-grid">
    <section class="glass-panel section-block placeholder-panel">
      <div class="placeholder-badge">功能预留</div>
      <h2 class="section-title">{{ pageTitle }}</h2>
      <p class="page-subtitle">{{ pageSubtitle }}</p>

      <div class="placeholder-copy">
        <p>{{ pageNote }}</p>
        <p>当前已完成菜单、路由和页面占位，后续可以在此基础上继续补齐真实业务实现。</p>
      </div>
    </section>
  </div>
</template>

<script setup>
import { computed } from 'vue'
import { useRoute } from 'vue-router'

const route = useRoute()

const pageTitle = computed(() => route.meta?.title || '功能建设中')
const pageSubtitle = computed(() => route.meta?.subtitle || '当前页面已完成结构预留。')
const pageNote = computed(() => route.meta?.placeholderNote || '该功能暂未实施。')
</script>

<style scoped>
.placeholder-panel {
  min-height: 320px;
  display: grid;
  align-content: center;
  gap: 16px;
  padding: 36px;
}

.placeholder-badge {
  width: fit-content;
  padding: 6px 12px;
  border-radius: 999px;
  background: var(--brand-soft);
  color: var(--brand);
  font-size: 13px;
  font-weight: 600;
}

.section-title {
  margin: 0;
  font-size: 28px;
  line-height: 1.3;
}

.page-subtitle,
.placeholder-copy p {
  margin: 0;
  color: var(--text-sub);
  line-height: 1.8;
}

.placeholder-copy {
  display: grid;
  gap: 8px;
  max-width: 720px;
}
</style>

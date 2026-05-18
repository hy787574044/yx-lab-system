<template>
  <div v-if="showPagination" class="table-pagination">
    <div class="table-pagination__summary">
      共 {{ total }} 条，当前第 {{ currentPage }} / {{ pageCount }} 页
    </div>

    <div class="table-pagination__controls">
      <el-select
        class="table-pagination__size-select"
        style="width: 128px"
        :model-value="normalizedPageSize"
        @change="handleSizeChange"
      >
        <el-option
          v-for="size in pageSizes"
          :key="size"
          :label="`${size}条/页`"
          :value="Number(size)"
        />
      </el-select>

      <div class="table-pagination__page-group">
        <el-button :disabled="currentPage <= 1" @click="changePage(currentPage - 1)">
          上一页
        </el-button>

        <template v-for="item in visiblePages" :key="`page-${item}`">
          <span v-if="item === '...'" class="table-pagination__ellipsis">...</span>
          <el-button
            v-else
            :type="item === currentPage ? 'primary' : 'default'"
            :plain="item !== currentPage"
            class="table-pagination__page"
            @click="changePage(item)"
          >
            {{ item }}
          </el-button>
        </template>

        <el-button :disabled="currentPage >= pageCount" @click="changePage(currentPage + 1)">
          下一页
        </el-button>
      </div>
    </div>
  </div>
</template>

<script setup>
import { computed } from 'vue'
import { ElButton } from 'element-plus/es/components/button/index.mjs'
import { ElOption, ElSelect } from 'element-plus/es/components/select/index.mjs'
import { DEFAULT_PAGE_SIZE, PAGE_SIZE_OPTIONS } from '../../utils/labEnums'

const props = defineProps({
  currentPage: {
    type: Number,
    default: 1
  },
  pageSize: {
    type: [Number, String],
    default: DEFAULT_PAGE_SIZE
  },
  pageSizes: {
    type: Array,
    default: () => PAGE_SIZE_OPTIONS
  },
  total: {
    type: Number,
    default: 0
  }
})

const emit = defineEmits(['update:currentPage', 'update:pageSize', 'change'])

const normalizedPageSize = computed(() => {
  const currentSize = Number(props.pageSize)
  if (Number.isFinite(currentSize) && currentSize > 0) {
    return currentSize
  }
  const fallbackSize = Number(props.pageSizes?.[0] ?? DEFAULT_PAGE_SIZE)
  return Number.isFinite(fallbackSize) && fallbackSize > 0 ? fallbackSize : DEFAULT_PAGE_SIZE
})

const pageCount = computed(() => Math.max(1, Math.ceil((props.total || 0) / normalizedPageSize.value)))
const showPagination = computed(() => props.total > 0)

const visiblePages = computed(() => {
  const totalPages = pageCount.value
  const current = props.currentPage

  if (totalPages <= 7) {
    return Array.from({ length: totalPages }, (_, index) => index + 1)
  }

  if (current <= 4) {
    return [1, 2, 3, 4, 5, '...', totalPages]
  }

  if (current >= totalPages - 3) {
    return [1, '...', totalPages - 4, totalPages - 3, totalPages - 2, totalPages - 1, totalPages]
  }

  return [1, '...', current - 1, current, current + 1, '...', totalPages]
})

function changePage(page) {
  if (page < 1 || page > pageCount.value || page === props.currentPage) {
    return
  }
  emit('update:currentPage', page)
  emit('change')
}

function handleSizeChange(size) {
  const nextSize = Number(size)
  if (!Number.isFinite(nextSize) || nextSize <= 0) {
    return
  }
  emit('update:pageSize', nextSize)
  emit('update:currentPage', 1)
  emit('change')
}
</script>

<style scoped>
.table-pagination {
  display: flex;
  align-items: center;
  justify-content: space-between;
  gap: 12px;
  margin-top: 0;
  padding: 10px 14px;
  border-top: 1px solid var(--line-soft);
  background: #ffffff;
}

.table-pagination__summary {
  flex: 0 0 auto;
  color: var(--text-sub);
  font-size: 12px;
  line-height: 32px;
  white-space: nowrap;
}

.table-pagination__controls {
  display: inline-flex;
  align-items: center;
  justify-content: flex-end;
  gap: 10px;
  flex: 0 0 auto;
  flex-wrap: nowrap;
}

.table-pagination__size-select {
  flex: 0 0 auto;
  width: 128px;
  min-width: 128px;
}

.table-pagination__size-select :deep(.el-select__wrapper) {
  min-height: 34px;
  border-radius: 8px;
  background: #ffffff;
  box-shadow: 0 0 0 1px #cfd9e6 inset;
}

.table-pagination__size-select :deep(.el-select__wrapper:hover) {
  box-shadow: 0 0 0 1px color-mix(in srgb, var(--brand) 50%, #ffffff 50%) inset;
}

.table-pagination__size-select :deep(.el-select__wrapper.is-focused) {
  box-shadow: 0 0 0 1px var(--brand) inset;
}

.table-pagination__size-select :deep(.el-select__selected-item),
.table-pagination__size-select :deep(.el-select__placeholder) {
  color: var(--text-main);
  font-size: 13px;
}

.table-pagination__size-select :deep(.el-select__caret) {
  color: var(--text-light);
}

.table-pagination__page-group {
  display: inline-flex;
  align-items: center;
  gap: 8px;
  flex-wrap: nowrap;
}

.table-pagination__page {
  min-width: 32px;
}

.table-pagination__ellipsis {
  padding: 0 4px;
  color: var(--text-light);
  font-size: 12px;
}

@media (max-width: 900px) {
  .table-pagination {
    align-items: flex-start;
    flex-direction: column;
  }

  .table-pagination__controls,
  .table-pagination__page-group {
    flex-wrap: wrap;
  }
}
</style>

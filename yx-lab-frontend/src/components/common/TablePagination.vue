<template>
  <div class="table-pagination" v-if="showPagination">
    <div class="table-pagination__summary">
      共 {{ total }} 条，当前第 {{ currentPage }} / {{ pageCount }} 页
    </div>

    <div class="table-pagination__controls">
      <span class="table-pagination__label">每页</span>
      <el-select
        :model-value="pageSize"
        class="table-pagination__size"
        @update:model-value="handleSizeChange"
      >
        <el-option
          v-for="size in pageSizes"
          :key="size"
          :label="`${size} 条/页`"
          :value="size"
        />
      </el-select>

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
</template>

<script setup>
import { computed } from 'vue'
import { DEFAULT_PAGE_SIZE, PAGE_SIZE_OPTIONS } from '../../utils/labEnums'

const props = defineProps({
  currentPage: {
    type: Number,
    default: 1
  },
  pageSize: {
    type: Number,
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

const pageCount = computed(() => Math.max(1, Math.ceil((props.total || 0) / (props.pageSize || 1))))
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
  emit('update:pageSize', size)
  emit('update:currentPage', 1)
  emit('change')
}
</script>

<style scoped>
.table-pagination {
  display: flex;
  align-items: center;
  justify-content: space-between;
  gap: 16px;
  margin-top: 0;
  padding: 12px 14px;
  border-top: 1px solid var(--line-soft);
  background: #ffffff;
}

.table-pagination__summary {
  color: var(--text-sub);
  font-size: 12px;
  white-space: nowrap;
}

.table-pagination__controls {
  display: flex;
  align-items: center;
  justify-content: flex-end;
  flex-wrap: wrap;
  gap: 8px;
}

.table-pagination__label {
  color: var(--text-sub);
  font-size: 12px;
}

.table-pagination__size {
  width: 112px;
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

  .table-pagination__controls {
    justify-content: flex-start;
  }
}
</style>

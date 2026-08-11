<template>
  <div
    class="m-order-card"
    :style="{ borderLeftColor: order.borderColor || '#e8e8e8' }"
  >
    <div class="m-order-top">
      <span class="m-order-no">{{ order.orderNo }}</span>
      <span class="m-order-amount">{{ order.amount }}</span>
    </div>
    <div class="m-order-meta">
      <span class="m-order-info">{{ order.productName }}</span>
      <span class="tag" :class="statusTagClass(order.status)" style="font-size:10px">
        {{ order.statusText }}
      </span>
    </div>
  </div>
</template>

<script setup>
defineProps({ order: { type: Object, required: true } })

function statusTagClass(status) {
  const map = { new: 'tag-blue', paid: 'tag-blue', callback: 'tag-green', refund: 'tag-red', expired: 'tag-gray', fail: 'tag-orange' }
  return map[status] || 'tag-gray'
}
</script>

<style scoped>
.m-order-card {
  background: #fff;
  border-radius: 10px;
  padding: 14px;
  margin-bottom: 10px;
  box-shadow: 0 1px 3px rgba(0, 0, 0, 0.06);
  border-left: 4px solid #e8e8e8;
  cursor: pointer;
  transition: background 0.15s;
}
.m-order-card:active {
  background: #fafafa;
}
.m-order-top {
  display: flex;
  justify-content: space-between;
  align-items: flex-start;
}
.m-order-no {
  font-size: 13px;
  font-weight: 500;
  color: #333;
}
.m-order-amount {
  font-size: 16px;
  font-weight: 700;
  color: #333;
}
.m-order-meta {
  display: flex;
  justify-content: space-between;
  align-items: center;
  margin-top: 6px;
}
.m-order-info {
  font-size: 11px;
  color: #999;
}
</style>

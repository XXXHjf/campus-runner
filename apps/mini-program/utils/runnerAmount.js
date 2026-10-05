// Normal orders collect the runner fee; purchase orders also reimburse goods.
function runnerReceivable(order = {}) {
  const fee = Number(order.price ?? 0);
  const goods = order.businessType === 'PURCHASE'
    ? Number(order.productAmount ?? order.product_amount ?? 0) : 0;
  return fee + goods;
}

function hasRunnerReceivable(order) {
  const amount = runnerReceivable(order);
  return Number.isFinite(amount) && amount > 0;
}

module.exports = { runnerReceivable, hasRunnerReceivable };

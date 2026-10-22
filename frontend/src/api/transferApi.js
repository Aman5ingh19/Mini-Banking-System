import api from './client';

export const transferApi = {
  executeTransfer: async (transferData, idempotencyKey) => {
    const headers = {};
    if (idempotencyKey) {
      headers['Idempotency-Key'] = idempotencyKey;
    }
    const res = await api.post('/transfers', transferData, { headers });
    return res.data;
  },
  getReceipt: async (referenceId) => {
    const res = await api.get(`/transfers/${referenceId}`);
    return res.data;
  },
};

import api from './client';

export const adminApi = {
  deposit: async (accountId, { amount, description }) => {
    const res = await api.post(`/admin/accounts/${accountId}/deposits`, { amount, description });
    return res.data;
  },
  withdraw: async (accountId, { amount, description }) => {
    const res = await api.post(`/admin/accounts/${accountId}/withdrawals`, { amount, description });
    return res.data;
  },
};

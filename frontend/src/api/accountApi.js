import api from './client';

export const accountApi = {
  getMyAccount: async () => {
    const res = await api.get('/accounts/me');
    return res.data;
  },
  getBalance: async (accountId) => {
    const res = await api.get(`/accounts/${accountId}/balance`);
    return res.data;
  },
  searchRecipients: async (query) => {
    const res = await api.get('/accounts/search', {
      params: { query },
    });
    return res.data;
  },
};

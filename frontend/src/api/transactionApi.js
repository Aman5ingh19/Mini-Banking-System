import api from './client';

export const transactionApi = {
  getTransactions: async ({ type = 'ALL', startDate, endDate, page = 0, size = 10 } = {}) => {
    const params = { type, page, size };
    if (startDate) params.startDate = startDate;
    if (endDate) params.endDate = endDate;
    const res = await api.get('/transactions', { params });
    return res.data;
  },
};

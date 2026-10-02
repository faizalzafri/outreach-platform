import { httpClient } from '@/lib/http-client';

/** Fetches a file from the API (with the user's token) and saves it under the given name. */
export async function downloadFile(path: string, fileName: string, params?: Record<string, string>): Promise<void> {
  const response = await httpClient.get<Blob>(path, { params, responseType: 'blob' });
  const url = URL.createObjectURL(response.data);
  const link = document.createElement('a');
  link.href = url;
  link.download = fileName;
  link.click();
  URL.revokeObjectURL(url);
}

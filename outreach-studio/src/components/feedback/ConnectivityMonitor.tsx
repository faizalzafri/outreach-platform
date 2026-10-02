/**
 * Connectivity Monitor
 *
 * Renders the appropriate banner (offline or system unavailable)
 * based on current connectivity state.
 */

import { useConnectivity } from '@/hooks/useConnectivity';
import { OfflineBanner } from './OfflineBanner';
import { SystemUnavailableBanner } from './SystemUnavailableBanner';

export function ConnectivityMonitor() {
  const { isOffline, isSystemUnavailable } = useConnectivity();

  if (isSystemUnavailable) {
    return <SystemUnavailableBanner />;
  }

  if (isOffline) {
    return <OfflineBanner />;
  }

  return null;
}

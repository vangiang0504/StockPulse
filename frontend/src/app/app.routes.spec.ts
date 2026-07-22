import { authGuard } from './core/guards/auth.guard';
import { WarehouseListComponent } from './features/warehouses/warehouse-list/warehouse-list.component';
import { routes } from './app.routes';

describe('application routes', () => {
  it('lazy-loads the Warehouse list under the protected main layout', async () => {
    const mainLayoutRoute = routes.find(route => route.path === '' && route.canActivate?.includes(authGuard));
    const warehouseRoute = mainLayoutRoute?.children?.find(route => route.path === 'warehouses');

    expect(mainLayoutRoute).toBeDefined();
    expect(warehouseRoute).toBeDefined();
    expect(warehouseRoute?.loadComponent).toBeDefined();
    expect(await warehouseRoute?.loadComponent?.()).toBe(WarehouseListComponent);
  });
});

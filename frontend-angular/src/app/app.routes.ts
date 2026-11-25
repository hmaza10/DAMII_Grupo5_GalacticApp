import { Routes } from '@angular/router';
import { LoginComponent } from './components/login/login.component';
import { DashboardComponent } from './components/dashboard/dashboard.component';
import { Categorias } from './components/categorias/categorias';
import { Platos } from './components/platos/platos';
import { Menus } from './components/menus/menus';
import { Clientes } from './components/clientes/clientes';
import { Pedidos } from './components/pedidos/pedidos';
import { Zonas } from './components/zonas/zonas';
import { sesionGuard } from './services/sesion.guard';

export const routes: Routes = [
  { path: '', redirectTo: 'dashboard', pathMatch: 'full' },
  { path: 'login', component: LoginComponent },
  { path: 'dashboard', component: DashboardComponent, canActivate: [sesionGuard] },
  { path: 'categorias', component: Categorias, canActivate: [sesionGuard] },
  { path: 'platos', component: Platos, canActivate: [sesionGuard] },
  { path: 'menus', component: Menus, canActivate: [sesionGuard] },
  { path: 'clientes', component: Clientes, canActivate: [sesionGuard] },
  { path: 'pedidos', component: Pedidos, canActivate: [sesionGuard] },
  { path: 'zonas', component: Zonas, canActivate: [sesionGuard] },
  { path: '**', redirectTo: 'dashboard' },
];

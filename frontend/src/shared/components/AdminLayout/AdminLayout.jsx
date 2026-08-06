import { useState } from 'react';
import { AdminSidebar } from './AdminSidebar';
import { AdminHeader } from './AdminHeader';

/**
 * Khung layout dung chung cho toan bo khu vuc Admin: sidebar trai (261px, co
 * dinh tren desktop, drawer tren mobile) + header tren cung + vung noi dung.
 *
 * Cach dung: bao trang admin bang component nay thay vi PublicHeader, vi du:
 *   <AdminLayout>
 *     <PageHeader title="Quản lý người dùng" ... />
 *     ... noi dung trang ...
 *   </AdminLayout>
 */
export function AdminLayout({ children }) {
  const [sidebarOpen, setSidebarOpen] = useState(false);

  return (
    <div className="min-h-screen bg-slate-50">
      <AdminSidebar isOpen={sidebarOpen} onClose={() => setSidebarOpen(false)} />

      <div className="lg:pl-64">
        <AdminHeader onOpenSidebar={() => setSidebarOpen(true)} />
        <main className="mx-auto max-w-7xl px-4 py-6 md:px-6 lg:px-8">{children}</main>
      </div>
    </div>
  );
}
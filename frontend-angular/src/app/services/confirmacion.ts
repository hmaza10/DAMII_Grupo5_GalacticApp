export interface OpcionesConfirmacion {
  titulo: string;
  texto?: string;
  confirmarTexto?: string;
  cancelarTexto?: string;
  icono?: 'question' | 'warning' | 'info';
}

interface ResultadoConfirmacion {
  isConfirmed: boolean;
}

interface ApiConfirmacion {
  fire: (opciones: Record<string, unknown>) => Promise<ResultadoConfirmacion>;
}

export function confirmarAccion(opciones: OpcionesConfirmacion): Promise<boolean> {
  const ventana = window as Window & { Swal?: ApiConfirmacion };
  const texto = opciones.texto ?? '';

  if (!ventana.Swal) {
    return Promise.resolve(window.confirm(`${opciones.titulo}${texto ? '\n\n' + texto : ''}`));
  }

  return ventana.Swal.fire({
    title: opciones.titulo,
    text: texto,
    icon: opciones.icono ?? 'question',
    showCancelButton: true,
    confirmButtonColor: '#e8871e',
    cancelButtonColor: '#6c757d',
    confirmButtonText: opciones.confirmarTexto ?? 'Confirmar',
    cancelButtonText: opciones.cancelarTexto ?? 'Cancelar'
  })
    .then((resultado) => resultado.isConfirmed)
    .catch(() => false);
}

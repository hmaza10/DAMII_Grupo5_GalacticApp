import { HttpErrorResponse } from '@angular/common/http';

export function mensajeErrorApi(error: unknown, respuestaPorDefecto: string): string {
  if (!(error instanceof HttpErrorResponse)) {
    return respuestaPorDefecto;
  }

  const cuerpo: unknown = error.error;

  if (typeof cuerpo === 'string' && cuerpo.trim().length > 0) {
    return cuerpo.trim();
  }

  if (cuerpo !== null && typeof cuerpo === 'object') {
    const registro = cuerpo as Record<string, unknown>;
    const mensaje = registro['mensaje'];

    if (typeof mensaje === 'string' && mensaje.trim().length > 0) {
      return mensaje.trim();
    }

    const detalles = Object.values(registro)
      .filter((valor): valor is string => typeof valor === 'string' && valor.trim().length > 0)
      .map((valor) => valor.trim());

    if (detalles.length > 0) {
      return detalles.join(' · ');
    }
  }

  return error.status === 0 ? 'No se pudo conectar con el servidor.' : respuestaPorDefecto;
}

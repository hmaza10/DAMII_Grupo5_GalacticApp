package com.galactica.util;

import com.galactica.entities.DetallePedido;
import com.galactica.entities.Pedido;
import com.lowagie.text.*;
import com.lowagie.text.Font;
import com.lowagie.text.pdf.PdfPCell;
import com.lowagie.text.pdf.PdfPTable;
import com.lowagie.text.pdf.PdfWriter;

import java.awt.*;
import java.io.ByteArrayOutputStream;
import java.math.BigDecimal;
import java.math.RoundingMode;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;

public class PedidoPdf {

    private static final Color AZUL_OSCURO = new Color(26, 26, 46);
    private static final Color GRIS_CLARO = new Color(242, 242, 242);

    private static final Font FUENTE_TITULO = new Font(Font.HELVETICA, 18, Font.BOLD, AZUL_OSCURO);
    private static final Font FUENTE_SUBTITULO = new Font(Font.HELVETICA, 10, Font.NORMAL, Color.GRAY);
    private static final Font FUENTE_ETIQUETA = new Font(Font.HELVETICA, 10, Font.BOLD, Color.DARK_GRAY);
    private static final Font FUENTE_TEXTO = new Font(Font.HELVETICA, 10, Font.NORMAL);
    private static final Font FUENTE_TABLA_CABECERA = new Font(Font.HELVETICA, 10, Font.BOLD, Color.WHITE);

    private static final DateTimeFormatter FORMATO_FECHA = DateTimeFormatter.ofPattern("dd/MM/yyyy");
    private static final DateTimeFormatter FORMATO_FECHA_HORA = DateTimeFormatter.ofPattern("dd/MM/yyyy HH:mm");

    public static byte[] generar(Pedido pedido) {
        try (ByteArrayOutputStream salida = new ByteArrayOutputStream()) {
            Document documento = new Document(PageSize.A4, 36, 36, 36, 36);
            PdfWriter.getInstance(documento, salida);
            documento.open();

            Paragraph titulo = new Paragraph("GALACTICA DELIVERY", FUENTE_TITULO);
            titulo.setAlignment(Element.ALIGN_CENTER);
            documento.add(titulo);

            Paragraph subtitulo = new Paragraph("Comprobante de pedido semanal", FUENTE_SUBTITULO);
            subtitulo.setAlignment(Element.ALIGN_CENTER);
            documento.add(subtitulo);

            Paragraph numero = new Paragraph("PEDIDO N " + pedido.getIdPedido(),
                    new Font(Font.HELVETICA, 14, Font.BOLD));
            numero.setAlignment(Element.ALIGN_CENTER);
            numero.setSpacingBefore(12);
            numero.setSpacingAfter(12);
            documento.add(numero);

            String cliente = "-";
            String telefono = "-";
            if (pedido.getCliente() != null) {
                telefono = nv1(pedido.getCliente().getTelefono());
                if (pedido.getCliente().getUsuario() != null) {
                    cliente = nv1(pedido.getCliente().getUsuario().getNombres() + " ") + nv1(pedido.getCliente().getUsuario().getApellidos());
                }
            }

            String semana = "-";
            if (pedido.getMenuSemanal() != null) {
                semana = formatearFecha(pedido.getMenuSemanal().getFechaInicio()) + " al " + formatearFecha(pedido.getMenuSemanal().getFechaFin());
            }

            PdfPTable tablaInfo = new PdfPTable(2);
            tablaInfo.setWidthPercentage(100);
            tablaInfo.setWidths(new float[]{0.35f, 0.65f});
            agregarFilaInfo(tablaInfo, "Fecha del pedido:", pedido.getFechaCreacion() == null ? "-" : pedido.getFechaCreacion().format(FORMATO_FECHA_HORA));
            agregarFilaInfo(tablaInfo, "Estado:", pedido.getEstado() == null ? "-" : pedido.getEstado().name());
            agregarFilaInfo(tablaInfo, "Semana del menú:", semana);
            agregarFilaInfo(tablaInfo, "Cliente:", cliente);
            agregarFilaInfo(tablaInfo, "Teléfono:", telefono);
            agregarFilaInfo(tablaInfo, "Dirección de entrega:", nv1(pedido.getDireccionEntrega()));
            agregarFilaInfo(tablaInfo, "Zona:", pedido.getZona() == null ? "-" : nv1(pedido.getZona().getNombre()));
            agregarFilaInfo(tablaInfo, "Horario de entrega:", nv1(pedido.getHorarioEntrega()));
            agregarFilaInfo(tablaInfo, "Método de pago:", nv1(pedido.getMetodoPago()));
            agregarFilaInfo(tablaInfo, "Observaciones:", pedido.getObservaciones().isBlank() ? "-" : pedido.getObservaciones());
            documento.add(tablaInfo);

            documento.add(new Paragraph(" ", FUENTE_TEXTO));

            PdfPTable tablaDetalles = new PdfPTable(5);
            tablaDetalles.setWidthPercentage(100);
            tablaDetalles.setWidths(new float[]{1.3f, 1.1f, 2.2f, 1.2f, 1.2f});
            agregarCeldaCabecera(tablaDetalles, "Día");
            agregarCeldaCabecera(tablaDetalles, "Fecha");
            agregarCeldaCabecera(tablaDetalles, "Plato");
            agregarCeldaCabecera(tablaDetalles, "Cantidad");
            agregarCeldaCabecera(tablaDetalles, "Subtotal (S/)");

            if (pedido.getDetalles() != null) {
                for (DetallePedido detalle : pedido.getDetalles()) {
                    agregarCeldaTexto(tablaDetalles, nv1(detalle.getDiaSemana()));
                    agregarCeldaTexto(tablaDetalles, formatearFecha(detalle.getFechaConsumo()));
                    agregarCeldaTexto(tablaDetalles, detalle.getPlato() == null ? "-" : nv1(detalle.getPlato().getNombre()));
                    agregarCeldaTexto(tablaDetalles, String.valueOf(detalle.getCantidad()));
                    agregarCeldaTexto(tablaDetalles, formatearMonto(detalle.getSubtotal()));
                }
            }
            documento.add(tablaDetalles);

            Paragraph delivery = new Paragraph("Delivery: S/ " + formatearMonto(pedido.getTarifaDelivery()), FUENTE_TEXTO);
            delivery.setAlignment(Element.ALIGN_RIGHT);
            delivery.setSpacingBefore(10);
            documento.add(delivery);

            Paragraph total = new Paragraph("TOTAL: S/ " + formatearMonto(pedido.getTotal()), new Font(Font.HELVETICA, 12, Font.BOLD, AZUL_OSCURO));
            total.setAlignment(Element.ALIGN_RIGHT);
            total.setSpacingBefore(4);
            documento.add(total);

            Paragraph pie = new Paragraph("Documento generado el " + LocalDateTime.now().format(FORMATO_FECHA_HORA),
                    FUENTE_SUBTITULO);
            pie.setAlignment(Element.ALIGN_CENTER);
            pie.setSpacingBefore(30);
            documento.add(pie);

            documento.close();
            return salida.toByteArray();
        } catch (Exception e) {
            throw new IllegalStateException("Error al generar el PDF del pedido", e);
        }
    }

    private static void agregarFilaInfo(PdfPTable tabla, String etiqueta, String valor) {
        PdfPCell celdaEtiqueta = new PdfPCell(new Phrase(etiqueta, FUENTE_ETIQUETA));
        celdaEtiqueta.setBackgroundColor(GRIS_CLARO);
        celdaEtiqueta.setBorderWidth(0.5f);
        celdaEtiqueta.setPadding(6);
        tabla.addCell(celdaEtiqueta);

        PdfPCell celdaValor = new PdfPCell(new Phrase(valor, FUENTE_TEXTO));
        celdaValor.setBorderWidth(0.5f);
        celdaValor.setPadding(6);
        tabla.addCell(celdaValor);
    }

    private static void agregarCeldaCabecera(PdfPTable tabla, String texto) {
        PdfPCell celda = new PdfPCell(new Phrase(texto, FUENTE_TABLA_CABECERA));
        celda.setBackgroundColor(AZUL_OSCURO);
        celda.setHorizontalAlignment(Element.ALIGN_CENTER);
        celda.setPadding(6);
        tabla.addCell(celda);
    }

    private static void agregarCeldaTexto(PdfPTable tabla, String texto) {
        PdfPCell celda = new PdfPCell(new Phrase(texto, FUENTE_TEXTO));
        celda.setPadding(6);
        tabla.addCell(celda);
    }

    private static String formatearFecha(LocalDate fecha) {
        return fecha == null ? "-" : fecha.format(FORMATO_FECHA);
    }

    private static String formatearMonto(BigDecimal monto) {
        return monto == null ? "0.00" : monto.setScale(2, RoundingMode.HALF_UP).toPlainString();
    }

    private static String nv1(String texto) {
        return texto == null ? "-" : texto;
    }
}

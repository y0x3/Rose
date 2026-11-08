# Capa `service`

Aquí vivirán los módulos que hablan con el exterior y que no pertenecen ni a la UI ni al dominio:

- `ai/`: interpretación de lenguaje natural y generación de acciones estructuradas.
- `voice/`: reconocimiento de voz y síntesis de texto.
- `notifications/`: notificaciones y recordatorios.
- `scheduling/`: trabajos programados con WorkManager.

En la fase 1 solo se reserva la estructura. Ningún servicio está implementado
todavía y ninguno modifica datos: las acciones que produzcan deberán pasar por
los casos de uso del dominio.

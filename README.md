# Inicio TV

Launcher Android ligero para TV Box con una pantalla de inicio panorámica, un acceso principal configurable y cuadrículas verticales para las apps favoritas y más aplicaciones. Usa **Elegir app** para seleccionar cuál aparece como acceso principal o volver a la selección automática. La cabecera reúne la marca, el Wi‑Fi, el almacenamiento disponible, la RAM libre, la hora y los ajustes. Mantén pulsado **OK** sobre una app para agregarla a favoritos, ocultarla o iniciar su desinstalación. La configuración incluye aplicaciones ocultas, información del dispositivo y temas de color. Android siempre muestra su propia confirmación al desinstalar.

El launcher no desactiva procesos ni modifica automáticamente los ajustes de rendimiento del sistema.

## Información del proyecto

- **Creador:** iappsan
- **Versión:** 1.0.0
- **Paquete Android:** `com.iappsan.tvboxlauncher`

## Compilar

Se requiere Java compatible con Gradle 9.6 y Android SDK Platform 37. En Windows, desde esta carpeta:

```powershell
.\gradlew.bat assembleDebug
```

El APK de prueba queda en `app\build\outputs\apk\debug\app-debug.apk`.

## Generar el release 1.0.0

```powershell
.\gradlew.bat assembleRelease
```

El APK Release sin firmar queda en `app\build\outputs\apk\release\app-release-unsigned.apk`. Antes de instalarlo o distribuirlo, hay que firmarlo con una keystore de release; conserva esa clave para poder publicar futuras actualizaciones.

El APK firmado queda en `app\build\outputs\apk\release\app-release.apk`. La keystore y los archivos locales de contraseña se guardan fuera del proyecto, en `%LOCALAPPDATA%\InicioTVSigning`; haz una copia segura de esos archivos y no los compartas ni los incluyas en el código fuente.


## Probar en el emulador

En VS Code, ejecuta la tarea **Iniciar emulador Android TV Box** desde **Terminal > Run Task**. El AVD `TVBoxTest` usa una imagen genérica Android 17/API 37 a 1080p; permite probar la interfaz y la navegación DPAD, pero no reproduce el hardware ni el rendimiento del X96Q.

Con el emulador iniciado, instala y abre el APK:

```powershell
adb -s emulator-5554 push app\build\outputs\apk\debug\app-debug.apk /data/local/tmp/inicio-tv.apk
adb -s emulator-5554 shell pm install -r /data/local/tmp/inicio-tv.apk
adb -s emulator-5554 shell am start -n com.iappsan.tvboxlauncher/.MainActivity
```

## Instalar por ADB

Activa **Depuración inalámbrica** en las opciones de desarrollador del TV Box. Empareja usando la IP y el puerto de emparejamiento que muestra el dispositivo; `adb pair` solicitará el código temporal:

```powershell
adb pair IP:PUERTO_DE_EMPAREJAMIENTO
adb connect IP:PUERTO_ADB
adb install -r app\build\outputs\apk\debug\app-debug.apk
```

Después, pulsa **Inicio/Home** en el control remoto y selecciona **Inicio TV** como aplicación de inicio. Si el fabricante no ofrece Depuración inalámbrica, hará falta conectar el TV Box al PC con un cable USB de datos compatible.

# HU-01 - Iniciar sesión en el sistema
## Historia: 
Como usuario del sistema (Administrador, Editor o Lector), quiero iniciar sesión mediante mis credenciales, para acceder de forma segura a las funcionalidades habilitadas para mi nivel de acceso.

## Descripción: 
El sistema debe exponer una pantalla de inicio de sesión donde el usuario ingresa su correo electrónico (o nombre de usuario) y contraseña. Ante credenciales válidas, el sistema debe generar una sesión autenticada e identificar el nivel de acceso del usuario para determinar las funcionalidades a las que podrá acceder durante el resto de la navegación.

## Criterios de Aceptación: 
- Dado que un usuario ingresa credenciales válidas (usuario y contraseña existentes y correctos), cuando presiona el botón de "Ingresar", entonces el sistema debe autenticarlo y redirigirlo a la pantalla principal correspondiente a su nivel de acceso (para todos, la pantalla principal es el tablero de control).
- Dado que un usuario ingresa credenciales inválidas o inexistentes, cuando intenta iniciar sesión, entonces el sistema debe rechazar el acceso y mostrar un mensaje de error genérico, sin especificar si el error corresponde al usuario o a la contraseña (por motivos de seguridad).
- Ante la falta de campos obligatorios en el formulario, al presionar "Ingresar", el sistema debe impedir la acción e indicar visualmente los campos requeridos.
- Dado que las contraseñas se almacenan en la base de datos, entonces estas deben ser persistidas utilizando un algoritmo de hash seguro (nunca en texto plano).

# HU-02 - Cerrar sesión del sistema
## Historia: 
Como usuario autenticado, quiero cerrar mi sesión de forma explícita, para proteger el acceso a la información cuando mi actividad en el sistema finaliza.

## Descripción: 
El sistema debe ofrecer, desde cualquier pantalla, una opción visible para cerrar sesión, invalidando el token o la sesión activa del usuario.

## Criterios de Aceptación: 
- Dado que un usuario autenticado presiona la opción "Cerrar sesión", cuando la acción se confirma, entonces el sistema debe invalidar su sesión y redirigirlo a la pantalla de inicio de sesión.
- Dado que la sesión de un usuario fue cerrada, cuando intenta acceder nuevamente a una pantalla protegida utilizando el navegador, entonces el sistema debe solicitarle un nuevo inicio de sesión.
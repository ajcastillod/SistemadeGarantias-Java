import java.util.*;
import java.io.*;
import java.nio.charset.StandardCharsets;
import java.util.regex.Pattern;
import java.time.LocalDate;
import java.time.format.DateTimeFormatter;
import java.util.function.Predicate;

public class GestionDeGarantias {
    private static final Scanner sc = new Scanner(new InputStreamReader(System.in, StandardCharsets.UTF_8));

    public static void main(String[] args) {
        Usuario usuarioActual = null;

        while (usuarioActual == null) {
            limpiarPantalla();
            mostrarBanner("SISTEMA DE GESTION DE GARANTIAS");
            System.out.println("1. Iniciar sesión");
            System.out.println("2. Registrarse");
            System.out.println("3. Salir");
            System.out.print("\nSeleccione una opción: ");
            String opcion = sc.nextLine();

            switch (opcion) {
                case "1" -> usuarioActual = Login.iniciarSesion(sc);
                case "2" -> Login.registrarUsuario(sc);
                case "3" -> {
                    System.out.println("\n¡Gracias por usar el sistema!");
                    return;
                }
                default -> {
                    System.out.println("\n Opción inválida. Presione Enter para continuar.");
                    sc.nextLine();
                }
            }
        }

        CentroServicio centro = new CentroServicio();
        String opcion;
        do {
            limpiarPantalla();
            mostrarBanner("MENU PRINCIPAL");
            System.out.println("1. Registrar nueva computadora");
            System.out.println("2. Mover computadora a Inspeccion");
            System.out.println("3. Mover computadora a Reparacion");
            System.out.println("4. Mover computadora a Control de Calidad");
            System.out.println("5. Mover computadora a Entrega");
            System.out.println("6. Mostrar historial de computadoras");
            System.out.println("7. Ver estado actual del sistema");
            System.out.println("8. Eliminar computadora (por error)");
            System.out.println("9. Salir");
            System.out.print("\nSeleccione una opción: ");
            opcion = sc.nextLine();

            switch (opcion) {
                case "1" -> {
                    Computadora comp = crearComputadora(sc, centro);
                    if (comp != null) {
                        centro.registrarComputadora(comp);
                    }
                }
                case "2" -> centro.moverAFase("inspeccion", sc);
                case "3" -> centro.moverAFase("reparacion", sc);
                case "4" -> centro.moverAFase("calidad", sc);
                case "5" -> centro.moverAFase("entrega", sc);
                case "6" -> {
                    limpiarPantalla();
                    mostrarBanner("HISTORIAL DE COMPUTADORAS");
                    centro.mostrarHistorial();
                    pausar(sc);
                }
                case "7" -> {
                    limpiarPantalla();
                    mostrarBanner("ESTADO ACTUAL DEL SISTEMA");
                    centro.mostrarEstadoActual();
                    pausar(sc);
                }
                case "8" -> {
                    centro.eliminarComputadoraPorTag(sc);
                    pausar(sc);
                }
                case "9" -> System.out.println(" Cerrando sesión...");
                default -> {
                    System.out.println(" Opción inválida.");
                    System.out.print("¿Desea regresar a la fase anterior? (s/n): ");
                    String resp = sc.nextLine().trim().toLowerCase();
                    if (!resp.equals("s")) {
                        opcion = "9"; // Forzar salida si no quiere regresar
                    }
                }
            }

            if (!opcion.equals("9") && !List.of("6","7","8").contains(opcion)) {
                System.out.print("\nPresione Enter para continuar...");
                sc.nextLine();
            }

        } while (!opcion.equals("9"));
    }

    /**
     * Método para crear una computadora solicitando datos al usuario con validación y opción de reintento.
     */
    public static Computadora crearComputadora(Scanner sc, CentroServicio centro) {
        limpiarPantalla();
        mostrarBanner("REGISTRAR COMPUTADORA");

        String tag = obtenerInputConValidacion(sc, "Service Tag: ",
                input -> !input.isEmpty() && !centro.existeServiceTag(input),
                "El Service Tag no puede estar vacío o ya existe.");

        if (tag == null) return null;

        String problema = obtenerInputConValidacion(sc, "Descripción del problema: ",
                input -> !input.isEmpty(),
                "La descripción no puede estar vacía.");

        if (problema == null) return null;

        String fecha = obtenerInputConValidacion(sc, "Fecha de recepción (DD-MM-YYYY): ",
                GestionDeGarantias::validarFechaDDMMYYYY,
                "Fecha no válida. Debe estar en formato DD-MM-YYYY");

        if (fecha == null) return null;

        fecha = convertirFechaGuatemalaDDMMYYYY(fecha);

        String nombre = obtenerInputConValidacion(sc, "Nombre del cliente: ",
                input -> !input.isEmpty(),
                "El nombre no puede estar vacío.");

        if (nombre == null) return null;

        String correo = obtenerInputConValidacion(sc, "Correo del cliente: ",
                input -> validarEmail(input) && !centro.existeCorreo(input),
                "Correo inválido o ya registrado.");

        if (correo == null) return null;

        String telefono = obtenerInputConValidacion(sc, "Teléfono del cliente: ",
                input -> validarTelefono(input) && !centro.existeTelefono(input),
                "Teléfono inválido (debe tener 8 a 10 dígitos) o ya registrado.");

        if (telefono == null) return null;

        return new Computadora(tag, problema, fecha, nombre, correo, telefono);
    }

    /**
     * Método auxiliar para obtener entrada con validación y opción de reintento o cancelación.
     */
    private static String obtenerInputConValidacion(Scanner sc, String prompt,
                                                    Predicate<String> validador,
                                                    String mensajeError) {
        while (true) {
            System.out.print(prompt);
            String input = sc.nextLine().trim();
            if (validador.test(input)) {
                return input;
            } else {
                System.out.println(mensajeError);
                System.out.print("¿Desea intentarlo nuevamente? (s/n): ");
                String respuesta = sc.nextLine().trim().toLowerCase();
                if (!respuesta.equals("s")) {
                    System.out.println("Operación cancelada.");
                    pausar(sc);
                    return null;
                }
            }
        }
    }

    public static void mostrarBanner(String titulo) {
        String tituloSinTilde = quitarTildes(titulo);
        System.out.println("╔════════════════════════════════════════════╗");
        System.out.printf("║ %-40s ║\n", tituloSinTilde.toUpperCase());
        System.out.println("╚════════════════════════════════════════════╝");
    }

    public static void limpiarPantalla() {
        try {
            String os = System.getProperty("os.name").toLowerCase();
            if (os.contains("windows")) {
                new ProcessBuilder("cmd", "/c", "cls").inheritIO().start().waitFor();
            } else {
                System.out.print("\033[H\033[2J");
                System.out.flush();
            }
        } catch (Exception e) {
            // En caso de error, no limpiar pero continuar
            System.out.println("Error al limpiar pantalla.");
        }
    }

    public static void pausar(Scanner sc) {
        System.out.print("\nPresione Enter para continuar...");
        sc.nextLine();
    }

    public static String quitarTildes(String input) {
        String original = "áéíóúÁÉÍÓÚ";
        String reemplazo = "aeiouAEIOU";
        StringBuilder sb = new StringBuilder();
        for (char c : input.toCharArray()) {
            int idx = original.indexOf(c);
            if (idx >= 0) sb.append(reemplazo.charAt(idx));
            else sb.append(c);
        }
        return sb.toString();
    }

    public static boolean validarEmail(String email) {
        String emailRegex = "^[a-zA-Z0-9._%+-]+@[a-zA-Z0-9.-]+\\.[a-zA-Z]{2,}$";
        return Pattern.matches(emailRegex, email);
    }

    public static boolean validarTelefono(String telefono) {
        String telefonoRegex = "^\\d{8,10}$";
        return Pattern.matches(telefonoRegex, telefono);
    }

    // Método para validar fecha en formato DD-MM-YYYY
    public static boolean validarFechaDDMMYYYY(String fechaStr) {
        try {
            DateTimeFormatter formatter = DateTimeFormatter.ofPattern("dd-MM-yyyy");
            LocalDate.parse(fechaStr, formatter);
            return true;
        } catch (Exception e) {
            return false;
        }
    }

    // Convertir fecha del formato DD-MM-YYYY a dd/MM/yyyy (formato Guatemala)
    public static String convertirFechaGuatemalaDDMMYYYY(String fechaStr) {
        try {
            DateTimeFormatter inputFormatter = DateTimeFormatter.ofPattern("dd-MM-yyyy");
            LocalDate fecha = LocalDate.parse(fechaStr, inputFormatter);
            DateTimeFormatter formatterGuate = DateTimeFormatter.ofPattern("dd/MM/yyyy");
            return fecha.format(formatterGuate);
        } catch (Exception e) {
            return fechaStr;
        }
    }
}

class Usuario {
    private String nombreUsuario;
    private String contraseña;

    public Usuario(String nombreUsuario, String contraseña) {
        this.nombreUsuario = nombreUsuario;
        this.contraseña = contraseña;
    }
    public String getNombreUsuario() { return nombreUsuario; }
    public String getContraseña() { return contraseña; }
}

class Login {
    private static final String ARCHIVO_USUARIOS = "usuarios.txt";

    public static Usuario iniciarSesion(Scanner sc) {
        GestionDeGarantias.limpiarPantalla();
        GestionDeGarantias.mostrarBanner("INICIAR SESION");

        System.out.print("Nombre de usuario: ");
        String nombre = sc.nextLine().trim();

        String contraseña = leerContraseña(sc, "Contraseña: ");

        List<Usuario> usuarios = cargarUsuarios();

        for (Usuario u : usuarios) {
            if (u.getNombreUsuario().equals(nombre) && u.getContraseña().equals(contraseña)) {
                System.out.println("\n ¡Bienvenido, " + nombre + "!");
                pausar(sc);
                return u;
            }
        }
        System.out.println("\n Usuario o contraseña incorrectos.");
        pausar(sc);
        return null;
    }

    public static void registrarUsuario(Scanner sc) {
        GestionDeGarantias.limpiarPantalla();
        GestionDeGarantias.mostrarBanner("REGISTRO DE USUARIO");

        String nombre;
        while (true) {
            System.out.print("Nuevo nombre de usuario: ");
            nombre = sc.nextLine().trim();
            if (nombre.isEmpty()) {
                System.out.println("\n Los campos no pueden estar vacíos.");
                continue;
            }
            if (usuarioExiste(nombre)) {
                System.out.println("\n El nombre de usuario ya está registrado.");
                continue;
            }
            break;
        }

        String contraseña;
        while (true) {
            contraseña = leerContraseña(sc, "Contraseña: ");
            System.out.print("Confirmar contraseña: ");
            String confirmacion = leerContraseña(sc, "");

            if (!contraseña.equals(confirmacion)) {
                System.out.println("\n Las contraseñas no coinciden. ¿Desea intentar nuevamente? (s/n): ");
                String respuesta = sc.nextLine().trim().toLowerCase();
                if (!respuesta.equals("s")) {
                    return;
                }
            } else {
                break;
            }
        }

        if (contraseña.isEmpty()) {
            System.out.println("\n La contraseña no puede estar vacía.");
            pausar(sc);
            return;
        }
        try (BufferedWriter bw = new BufferedWriter(new OutputStreamWriter(
                new FileOutputStream(ARCHIVO_USUARIOS, true), StandardCharsets.UTF_8))) {
            bw.write(nombre + "," + contraseña);
            bw.newLine();
            System.out.println("\n Usuario registrado con éxito.");
        } catch (IOException e) {
            System.out.println("\n Error al guardar el usuario.");
        }
        pausar(sc);
    }

    private static String leerContraseña(Scanner sc, String prompt) {
        Console consola = System.console();
        if (consola != null) {
            char[] pass = consola.readPassword(prompt);
            return new String(pass);
        } else {
            System.out.print(prompt);
            return sc.nextLine();
        }
    }

    private static List<Usuario> cargarUsuarios() {
        List<Usuario> usuarios = new ArrayList<>();
        File archivo = new File(ARCHIVO_USUARIOS);
        if (!archivo.exists()) return usuarios;

        try (BufferedReader br = new BufferedReader(new InputStreamReader(
                new FileInputStream(archivo), StandardCharsets.UTF_8))) {
            String linea;
            while ((linea = br.readLine()) != null) {
                String[] partes = linea.split(",");
                if (partes.length == 2) {
                    usuarios.add(new Usuario(partes[0], partes[1]));
                }
            }
        } catch (IOException e) {
            System.out.println("Error al leer el archivo de usuarios.");
        }
        return usuarios;
    }

    private static boolean usuarioExiste(String nombre) {
        List<Usuario> usuarios = cargarUsuarios();
        for (Usuario u : usuarios) {
            if (u.getNombreUsuario().equalsIgnoreCase(nombre)) {
                return true;
            }
        }
        return false;
    }
    private static void pausar(Scanner sc) {
        System.out.print("\nPresione Enter para continuar...");
        sc.nextLine();
    }
}

class Computadora {
    private String serviceTag;
    private String descripcionProblema;
    private String fechaRecepcion;
    private String nombreCliente;
    private String correoCliente;
    private String telefonoCliente;

    private String diagnostico = "";
    private String tecnico = "";
    private String resultadoReparacion = "";
    private String faseActual = "Recepción";

    public Computadora(String serviceTag, String descripcionProblema, String fechaRecepcion,
                       String nombreCliente, String correoCliente, String telefonoCliente) {
        this.serviceTag = serviceTag;
        this.descripcionProblema = descripcionProblema;
        this.fechaRecepcion = fechaRecepcion;
        this.nombreCliente = nombreCliente;
        this.correoCliente = correoCliente;
        this.telefonoCliente = telefonoCliente;
    }

    public String getServiceTag() { return serviceTag; }
    public String getFaseActual() { return faseActual; }
    public void setFaseActual(String fase) { this.faseActual = fase; }
    public void setDiagnostico(String diag) { this.diagnostico = diag; }
    public void setTecnico(String tecnico) { this.tecnico = tecnico; }
    public void setResultadoReparacion(String resultado) { this.resultadoReparacion = resultado; }
    public String getDescripcionProblema() { return descripcionProblema; }
    public String getFechaRecepcion() { return fechaRecepcion; }
    public String getNombreCliente() { return nombreCliente; }
    public String getCorreoCliente() { return correoCliente; }
    public String getTelefonoCliente() { return telefonoCliente; }

    public String resumen() {
        return "ServiceTag: " + serviceTag + " | Cliente: " + nombreCliente + " | Fase: " + faseActual;
    }

    public String historialCompleto() {
        return """
            ----------------------------------------
            ServiceTag: %s
            Cliente: %s
            Fecha de recepción: %s
            Problema: %s
            Diagnóstico: %s
            Técnico: %s
            Resultado reparación: %s
            Estado actual: %s
            ----------------------------------------
            """.formatted(serviceTag, nombreCliente, fechaRecepcion, descripcionProblema,
                          diagnostico, tecnico, resultadoReparacion, faseActual);
    }
}

class CentroServicio {
    private Queue<Computadora> recepcion = new LinkedList<>();
    private Queue<Computadora> inspeccion = new LinkedList<>();
    private Queue<Computadora> reparacion = new LinkedList<>();
    private Queue<Computadora> controlCalidad = new LinkedList<>();
    private Queue<Computadora> entrega = new LinkedList<>();

    private List<Computadora> historial = new ArrayList<>();

    private static final String ARCHIVO_HISTORIAL = "historial.txt";

    public void registrarComputadora(Computadora comp) {
        recepcion.add(comp);
        historial.add(comp);
        System.out.println("\n Computadora registrada en fase de Recepción.");
    }

    public boolean existeServiceTag(String tag) {
        for (Computadora c : historial) {
            if (c.getServiceTag().equalsIgnoreCase(tag)) return true;
        }
        return false;
    }

    public boolean existeCorreo(String correo) {
        for (Computadora c : historial) {
            if (c.getCorreoCliente().equalsIgnoreCase(correo)) return true;
        }
        return false;
    }

    public boolean existeTelefono(String telefono) {
        for (Computadora c : historial) {
            if (c.getTelefonoCliente().equals(telefono)) return true;
        }
        return false;
    }

    public void moverAFase(String fase, Scanner sc) {
        Queue<Computadora> origen = null, destino = null;
        String nombreFase = "";
        switch (fase.toLowerCase()) {
            case "inspeccion" -> {
                origen = recepcion;
                destino = inspeccion;
                nombreFase = "Inspección";
            }
            case "reparacion" -> {
                origen = inspeccion;
                destino = reparacion;
                nombreFase = "Reparación";
            }
            case "calidad" -> {
                origen = reparacion;
                destino = controlCalidad;
                nombreFase = "Control de Calidad";
            }
            case "entrega" -> {
                origen = controlCalidad;
                destino = entrega;
                nombreFase = "Entrega";
            }
            default -> {
                System.out.println(" Fase no válida.");
                GestionDeGarantias.pausar(sc);
                return;
            }
        }

        if (origen.isEmpty()) {
            System.out.println(" No hay computadoras en la fase anterior.");
            GestionDeGarantias.pausar(sc);
            return;
        }

        List<Computadora> listaDisponibles = new ArrayList<>(origen);
        System.out.println("\nComputadoras en fase " + nombreFase + " (elija ServiceTag para mover):");
        for (Computadora c : listaDisponibles) {
            System.out.println(" - " + c.resumen());
        }
        System.out.print("Ingrese ServiceTag de la computadora a mover (o '0' para cancelar): ");
        String elegido = sc.nextLine().trim();
        if (elegido.equals("0")) {
            System.out.println("Operación cancelada.");
            GestionDeGarantias.pausar(sc);
            return;
        }
        Computadora comp = null;
        for (Computadora c : listaDisponibles) {
            if (c.getServiceTag().equalsIgnoreCase(elegido)) {
                comp = c;
                break;
            }
        }
        if (comp == null) {
            System.out.println("No se encontró computadora con el ServiceTag especificado.");
            GestionDeGarantias.pausar(sc);
            return;
        }

        boolean removed = origen.remove(comp);
        if (!removed) {
            System.out.println("Error al remover la computadora de la cola origen.");
            GestionDeGarantias.pausar(sc);
            return;
        }

        comp.setFaseActual(nombreFase);

        if (fase.equals("inspeccion")) {
            System.out.print("Diagnóstico: ");
            comp.setDiagnostico(sc.nextLine());
        } else if (fase.equals("reparacion")) {
            System.out.print("Técnico responsable: ");
            comp.setTecnico(sc.nextLine());
            System.out.print("Resultado de la reparación: ");
            comp.setResultadoReparacion(sc.nextLine());
        } else if (fase.equals("calidad")) {
            controlCalidadDecision(comp, sc);
            if (comp.getFaseActual().equals("Reparación")) {
                GestionDeGarantias.pausar(sc);
                return;
            }
        }

        destino.add(comp);
        System.out.println(" Computadora movida a la fase: " + nombreFase);

        if (fase.equals("entrega")) {
            guardarHistorialEnArchivo(comp);
        }
        GestionDeGarantias.pausar(sc);
    }

    private void controlCalidadDecision(Computadora comp, Scanner sc) {
        System.out.print("¿El equipo fue entregado correctamente? (s/n): ");
        String respuesta = sc.nextLine().trim().toLowerCase();
        if (respuesta.equals("n")) {
            System.out.print("Especificar el inconveniente: ");
            String inconveniente = sc.nextLine();
            System.out.println("Regresando computadora a Reparación...");
            comp.setFaseActual("Reparación");
            reparacion.add(comp);
            System.out.println("Inconveniente registrado: " + inconveniente);
        }
    }

    private void guardarHistorialEnArchivo(Computadora comp) {
        try (BufferedWriter bw = new BufferedWriter(new FileWriter(ARCHIVO_HISTORIAL, true))) {
            bw.write(comp.historialCompleto());
            bw.newLine();
            System.out.println(" Historial guardado en archivo.");
        } catch (IOException e) {
            System.out.println(" Error al guardar el historial.");
        }
    }

    public void mostrarHistorial() {
        if (historial.isEmpty()) {
            System.out.println(" No hay computadoras registradas.");
            return;
        }

        for (Computadora comp : historial) {
            System.out.println(comp.historialCompleto());
        }
    }

    public void mostrarEstadoActual() {
        mostrarCola("Recepción", recepcion);
        mostrarCola("Inspección", inspeccion);
        mostrarCola("Reparación", reparacion);
        mostrarCola("Control de Calidad", controlCalidad);
        mostrarCola("Entrega", entrega);
    }

    private void mostrarCola(String nombre, Queue<Computadora> cola) {
        System.out.println("\n Fase: " + nombre);
        if (cola.isEmpty()) {
            System.out.println("   (Vacía)");
        } else {
            for (Computadora comp : cola) {
                System.out.println("   - " + comp.resumen());
            }
        }
    }

    public void eliminarComputadoraPorTag(Scanner sc) {
        System.out.print("Ingrese el ServiceTag a eliminar: ");
        String tag = sc.nextLine().trim();

        if (eliminarDeCola(recepcion, tag) || eliminarDeCola(inspeccion, tag)) {
            System.out.println(" Computadora eliminada correctamente.");
        } else {
            System.out.println(" No se encontró una computadora con ese ServiceTag en Recepción o Inspección.");
        }
    }

    private boolean eliminarDeCola(Queue<Computadora> cola, String tag) {
        Iterator<Computadora> it = cola.iterator();
        while (it.hasNext()) {
            if (it.next().getServiceTag().equalsIgnoreCase(tag)) {
                it.remove();
                return true;
            }
        }
        return false;
    }
}	
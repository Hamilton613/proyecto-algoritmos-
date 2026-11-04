import javax.swing.*;
import javax.swing.table.DefaultTableModel;
import java.awt.*;
import java.io.*;
import java.time.LocalDate;
import java.util.*;

public class Main {
    public static void main(String[] args) {
        SwingUtilities.invokeLater(() -> {
            try { UIManager.setLookAndFeel(UIManager.getSystemLookAndFeelClassName()); } catch (Exception ignored) {}
            new VentanaPrincipal().setVisible(true);
        });
    }
}

// producto, cliente, empleado:Jefry.
class Producto implements Serializable {
    String codigo, nombre, descripcion, marca; double precio;
    Producto(String codigo, String nombre, String descripcion, String marca, double precio) {
        this.codigo=codigo; this.nombre=nombre; this.descripcion=descripcion; this.marca=marca; this.precio=precio;
    }
}
class Cliente implements Serializable {
    String dpi, nombre, direccion, fechaNacimiento, tipo;
    Cliente(String dpi,String nombre,String direccion,String fechaNacimiento,String tipo){this.dpi=dpi;this.nombre=nombre;this.direccion=direccion;this.fechaNacimiento=fechaNacimiento;this.tipo=tipo;}
}
class Empleado implements Serializable {
    String id,nombre,dpi,direccion,puesto; double salario;
    Empleado(String id,String nombre,String dpi,String direccion,String puesto,double salario){this.id=id;this.nombre=nombre;this.dpi=dpi;this.direccion=direccion;this.puesto=puesto;this.salario=salario;}
}
//bodega, existencia, linea de orden:Javier 
class Bodega implements Serializable {
    String id,empleadoId,direccion; int capacidad;
    Bodega(String id,String empleadoId,int capacidad,String direccion){this.id=id;this.empleadoId=empleadoId;this.capacidad=capacidad;this.direccion=direccion;}
}
class Existencia implements Serializable {
    String productoCodigo,bodegaId; int cantidad;
    Existencia(String productoCodigo,String bodegaId,int cantidad){this.productoCodigo=productoCodigo;this.bodegaId=bodegaId;this.cantidad=cantidad;}
}
class LineaOrden implements Serializable {
    String productoCodigo; int cantidad;
    LineaOrden(String productoCodigo,int cantidad){this.productoCodigo=productoCodigo;this.cantidad=cantidad;}
}
class OrdenCompra implements Serializable {
    String codigo,clienteDpi,fecha,estado; ArrayList<LineaOrden> lineas; double total;
    OrdenCompra(String codigo,String clienteDpi,String fecha,ArrayList<LineaOrden> lineas,double total){this.codigo=codigo;this.clienteDpi=clienteDpi;this.fecha=fecha;this.lineas=lineas;this.total=total;this.estado="Pendiente";}
}

class AlmacenDatos {
    private static final String CARPETA = "datos";
    static {
        File dir=new File(CARPETA);
        if(!dir.exists()) dir.mkdirs();
    }
    @SuppressWarnings("unchecked")
    static <T> ArrayList<T> leer(String archivo) {
        File f=new File(CARPETA+File.separator+archivo);
        if(!f.exists()) return new ArrayList<>();
        try(ObjectInputStream in=new ObjectInputStream(new FileInputStream(f))) {
            return (ArrayList<T>) in.readObject();
        } catch(Exception e) { JOptionPane.showMessageDialog(null,"No se pudo leer "+archivo+": "+e.getMessage()); return new ArrayList<>(); }
    }
    static void guardar(String archivo,Object datos) {
        try(ObjectOutputStream out=new ObjectOutputStream(new FileOutputStream(CARPETA+File.separator+archivo))) {
            out.writeObject(datos);
        } catch(IOException e) { JOptionPane.showMessageDialog(null,"No se pudo guardar "+archivo+": "+e.getMessage()); }
    }
}

class SistemaTienda {
    ArrayList<Producto> productos=AlmacenDatos.leer("productos.dat");
    ArrayList<Cliente> clientes=AlmacenDatos.leer("clientes.dat");
    ArrayList<Empleado> empleados=AlmacenDatos.leer("empleados.dat");
    ArrayList<Bodega> bodegas=AlmacenDatos.leer("bodegas.dat");
    ArrayList<Existencia> inventario=AlmacenDatos.leer("inventario.dat");
    ArrayList<OrdenCompra> ordenes=AlmacenDatos.leer("ordenes.dat");

    void guardarProductos(){AlmacenDatos.guardar("productos.dat",productos);}
    void guardarClientes(){AlmacenDatos.guardar("clientes.dat",clientes);}
    void guardarEmpleados(){AlmacenDatos.guardar("empleados.dat",empleados);}
    void guardarBodegas(){AlmacenDatos.guardar("bodegas.dat",bodegas);}
    void guardarInventario(){AlmacenDatos.guardar("inventario.dat",inventario);}
    void guardarOrdenes(){AlmacenDatos.guardar("ordenes.dat",ordenes);}

    Producto producto(String codigo){for(Producto p:productos)if(p.codigo.equalsIgnoreCase(codigo))return p;return null;}
    Cliente cliente(String dpi){for(Cliente c:clientes)if(c.dpi.equals(dpi))return c;return null;}
    Empleado empleado(String id){for(Empleado e:empleados)if(e.id.equalsIgnoreCase(id))return e;return null;}
    Bodega bodega(String id){for(Bodega b:bodegas)if(b.id.equalsIgnoreCase(id))return b;return null;}
    int stock(String codigo){int n=0;for(Existencia x:inventario)if(x.productoCodigo.equalsIgnoreCase(codigo))n+=x.cantidad;return n;}
    void registrarProducto(Producto p){if(producto(p.codigo)!=null)throw new IllegalArgumentException("Ya existe ese código de producto.");productos.add(p);guardarProductos();}
    void registrarCliente(Cliente c){if(cliente(c.dpi)!=null)throw new IllegalArgumentException("Ya existe un cliente con ese DPI.");clientes.add(c);guardarClientes();}
    void registrarEmpleado(Empleado e){if(empleado(e.id)!=null)throw new IllegalArgumentException("Ya existe ese ID de empleado.");empleados.add(e);guardarEmpleados();}
    void registrarBodega(Bodega b){if(bodega(b.id)!=null)throw new IllegalArgumentException("Ya existe ese ID de bodega.");if(empleado(b.empleadoId)==null)throw new IllegalArgumentException("Primero registre el empleado responsable.");bodegas.add(b);guardarBodegas();}
    void registrarExistencia(Existencia x){if(producto(x.productoCodigo)==null)throw new IllegalArgumentException("El producto no existe.");if(bodega(x.bodegaId)==null)throw new IllegalArgumentException("La bodega no existe.");if(x.cantidad<=0)throw new IllegalArgumentException("La cantidad debe ser mayor que cero.");for(Existencia e:inventario)if(e.productoCodigo.equalsIgnoreCase(x.productoCodigo)&&e.bodegaId.equalsIgnoreCase(x.bodegaId)){e.cantidad+=x.cantidad;guardarInventario();return;}inventario.add(x);guardarInventario();}
    OrdenCompra crearOrden(String codigo,String dpi,ArrayList<LineaOrden> lineas){
        if(cliente(dpi)==null)throw new IllegalArgumentException("El cliente no está registrado.");
        if(lineas.isEmpty())throw new IllegalArgumentException("La orden debe incluir al menos un producto.");
        for(OrdenCompra o:ordenes)if(o.codigo.equalsIgnoreCase(codigo))throw new IllegalArgumentException("Ya existe ese código de orden.");
        double total=0;
        for(LineaOrden l:lineas){Producto p=producto(l.productoCodigo);if(p==null)throw new IllegalArgumentException("No existe el producto: "+l.productoCodigo);if(l.cantidad<=0)throw new IllegalArgumentException("La cantidad debe ser positiva.");if(stock(l.productoCodigo)<l.cantidad)throw new IllegalArgumentException("Stock insuficiente para "+p.nombre+". Disponible: "+stock(l.productoCodigo));total+=p.precio*l.cantidad;}
        OrdenCompra o=new OrdenCompra(codigo,dpi,LocalDate.now().toString(),lineas,total);ordenes.add(o);guardarOrdenes();return o;
    }
    void completarOrden(String codigo){
        OrdenCompra o=null;for(OrdenCompra x:ordenes)if(x.codigo.equalsIgnoreCase(codigo)){o=x;break;}
        if(o==null)throw new IllegalArgumentException("No se encontró la orden.");
        if(o.estado.equals("Completada"))throw new IllegalArgumentException("Esta orden ya fue completada.");
        if(o.estado.equals("Cancelada"))throw new IllegalArgumentException("No se puede completar una orden cancelada.");
        // Validar todo antes de descontar para evitar ventas parciales.
        for(LineaOrden l:o.lineas)if(stock(l.productoCodigo)<l.cantidad)throw new IllegalArgumentException("Stock insuficiente para completar la orden: "+l.productoCodigo);
        for(LineaOrden l:o.lineas){int restante=l.cantidad;for(Existencia x:inventario){if(x.productoCodigo.equalsIgnoreCase(l.productoCodigo)&&restante>0){int descuento=Math.min(x.cantidad,restante);x.cantidad-=descuento;restante-=descuento;}}}
        o.estado="Completada";guardarInventario();guardarOrdenes();
    }
    void cancelarOrden(String codigo){for(OrdenCompra o:ordenes)if(o.codigo.equalsIgnoreCase(codigo)){if(o.estado.equals("Completada"))throw new IllegalArgumentException("Una orden completada no se puede cancelar desde esta opción.");o.estado="Cancelada";guardarOrdenes();return;}throw new IllegalArgumentException("No se encontró la orden.");}
}

class VentanaPrincipal extends JFrame {
    final SistemaTienda sistema=new SistemaTienda();
    final Color MORADO=new Color(120,69,216), FONDO=new Color(25,27,41);
    VentanaPrincipal(){
        setTitle("tienda de juegos");setSize(1050,650);setLocationRelativeTo(null);setDefaultCloseOperation(EXIT_ON_CLOSE);
        getContentPane().setBackground(new Color(16,17,26));setLayout(new BorderLayout());
        JLabel titulo=new JLabel("tienda de juegos",SwingConstants.CENTER);titulo.setOpaque(true);titulo.setBackground(FONDO);titulo.setForeground(new Color(169,112,255));titulo.setFont(new Font("Arial",Font.BOLD,24));titulo.setBorder(BorderFactory.createEmptyBorder(20,10,20,10));add(titulo,BorderLayout.NORTH);
        JTabbedPane tabs=new JTabbedPane();tabs.setFont(new Font("Arial",Font.BOLD,14));
        tabs.addTab("Productos",panelProductos());tabs.addTab("Clientes",panelClientes());tabs.addTab("Empleados",panelEmpleados());tabs.addTab("Bodegas",panelBodegas());tabs.addTab("Inventario",panelInventario());tabs.addTab("Órdenes / Ventas",panelOrdenes());add(tabs,BorderLayout.CENTER);
        JLabel pie=new JLabel("Los registros se guardan automáticamente en la carpeta datos",SwingConstants.CENTER);pie.setForeground(Color.LIGHT_GRAY);pie.setBorder(BorderFactory.createEmptyBorder(8,5,8,5));add(pie,BorderLayout.SOUTH);
    }
    JPanel basePanel(){JPanel p=new JPanel(new BorderLayout(8,8));p.setBackground(new Color(16,17,26));p.setBorder(BorderFactory.createEmptyBorder(12,12,12,12));return p;}
    JButton boton(String s){
        JButton b=new JButton(s);
        b.setBackground(new Color(91,33,182));
        b.setForeground(Color.WHITE);
        b.setFont(new Font("Arial",Font.BOLD,14));
        b.setFocusPainted(false);
        b.setBorderPainted(false);
        b.setOpaque(true);
        b.setPreferredSize(new Dimension(190,42));
        return b;
    }
    DefaultTableModel modelo(String... cols){return new DefaultTableModel(cols,0){public boolean isCellEditable(int r,int c){return false;}};}
    JTable tabla(DefaultTableModel m){JTable t=new JTable(m);t.setRowHeight(24);return t;}
    void mensaje(Runnable r){try{r.run();}catch(Exception e){JOptionPane.showMessageDialog(this,e.getMessage(),"Aviso",JOptionPane.WARNING_MESSAGE);}}
    void refrescar(DefaultTableModel m,Object[][] filas){m.setRowCount(0);for(Object[] f:filas)m.addRow(f);}
    JPanel panelProductos(){
        JPanel p=basePanel();DefaultTableModel m=modelo("Código","Nombre","Descripción","Marca","Precio (Q)");JTable t=tabla(m);p.add(new JScrollPane(t),BorderLayout.CENTER);
        JButton agregar=boton("Registrar producto"), actualizar=boton("Actualizar lista");JPanel sur=new JPanel(new FlowLayout(FlowLayout.CENTER,25,12));sur.setBackground(new Color(16,17,26));sur.add(agregar);sur.add(actualizar);p.add(sur,BorderLayout.SOUTH);
        Runnable cargar=()->{m.setRowCount(0);for(Producto x:sistema.productos)m.addRow(new Object[]{x.codigo,x.nombre,x.descripcion,x.marca,x.precio});};cargar.run();actualizar.addActionListener(e->cargar.run());
        agregar.addActionListener(e->{JTextField c=new JTextField(),n=new JTextField(),d=new JTextField(),ma=new JTextField(),pr=new JTextField();Object[] f={"Código:",c,"Nombre:",n,"Descripción:",d,"Marca:",ma,"Precio:",pr};if(JOptionPane.showConfirmDialog(this,f,"Nuevo producto",JOptionPane.OK_CANCEL_OPTION)==0)mensaje(()->{sistema.registrarProducto(new Producto(c.getText().trim(),n.getText().trim(),d.getText().trim(),ma.getText().trim(),Double.parseDouble(pr.getText())));cargar.run();});});return p;
    }
    JPanel panelClientes(){
        JPanel p=basePanel();DefaultTableModel m=modelo("DPI","Nombre","Dirección","Nacimiento","Tipo");p.add(new JScrollPane(tabla(m)),BorderLayout.CENTER);JButton a=boton("Registrar cliente"),r=boton("Actualizar lista");JPanel s=new JPanel();s.setBackground(new Color(16,17,26));s.add(a);s.add(r);p.add(s,BorderLayout.SOUTH);Runnable cargar=()->{m.setRowCount(0);for(Cliente x:sistema.clientes)m.addRow(new Object[]{x.dpi,x.nombre,x.direccion,x.fechaNacimiento,x.tipo});};cargar.run();r.addActionListener(e->cargar.run());a.addActionListener(e->{JTextField dpi=new JTextField(),nom=new JTextField(),dir=new JTextField(),fec=new JTextField();JComboBox<String> tipo=new JComboBox<>(new String[]{"Regular","VIP"});Object[] f={"DPI:",dpi,"Nombre completo:",nom,"Dirección:",dir,"Fecha nacimiento (AAAA-MM-DD):",fec,"Tipo:",tipo};if(JOptionPane.showConfirmDialog(this,f,"Nuevo cliente",JOptionPane.OK_CANCEL_OPTION)==0)mensaje(()->{sistema.registrarCliente(new Cliente(dpi.getText().trim(),nom.getText().trim(),dir.getText().trim(),fec.getText().trim(),tipo.getSelectedItem().toString()));cargar.run();});});return p;
    }
    JPanel panelEmpleados(){
        JPanel p=basePanel();DefaultTableModel m=modelo("ID","Nombre","DPI","Dirección","Puesto","Salario");p.add(new JScrollPane(tabla(m)),BorderLayout.CENTER);JButton a=boton("Registrar empleado"),r=boton("Actualizar lista");JPanel s=new JPanel();s.setBackground(new Color(16,17,26));s.add(a);s.add(r);p.add(s,BorderLayout.SOUTH);Runnable cargar=()->{m.setRowCount(0);for(Empleado x:sistema.empleados)m.addRow(new Object[]{x.id,x.nombre,x.dpi,x.direccion,x.puesto,x.salario});};cargar.run();r.addActionListener(e->cargar.run());a.addActionListener(e->{JTextField id=new JTextField(),n=new JTextField(),dpi=new JTextField(),d=new JTextField(),pu=new JTextField(),sa=new JTextField();Object[] f={"ID:",id,"Nombre:",n,"DPI:",dpi,"Dirección:",d,"Puesto:",pu,"Salario:",sa};if(JOptionPane.showConfirmDialog(this,f,"Nuevo empleado",JOptionPane.OK_CANCEL_OPTION)==0)mensaje(()->{sistema.registrarEmpleado(new Empleado(id.getText().trim(),n.getText().trim(),dpi.getText().trim(),d.getText().trim(),pu.getText().trim(),Double.parseDouble(sa.getText())));cargar.run();});});return p;
    }
    JPanel panelBodegas(){
        JPanel p=basePanel();DefaultTableModel m=modelo("ID","Empleado responsable","Capacidad","Dirección");p.add(new JScrollPane(tabla(m)),BorderLayout.CENTER);JButton a=boton("Registrar bodega"),r=boton("Actualizar lista");JPanel s=new JPanel();s.setBackground(new Color(16,17,26));s.add(a);s.add(r);p.add(s,BorderLayout.SOUTH);Runnable cargar=()->{m.setRowCount(0);for(Bodega x:sistema.bodegas)m.addRow(new Object[]{x.id,x.empleadoId,x.capacidad,x.direccion});};cargar.run();r.addActionListener(e->cargar.run());a.addActionListener(e->{JTextField id=new JTextField(),emp=new JTextField(),cap=new JTextField(),dir=new JTextField();Object[] f={"ID bodega:",id,"ID empleado responsable:",emp,"Capacidad (unidades):",cap,"Dirección:",dir};if(JOptionPane.showConfirmDialog(this,f,"Nueva bodega",JOptionPane.OK_CANCEL_OPTION)==0)mensaje(()->{sistema.registrarBodega(new Bodega(id.getText().trim(),emp.getText().trim(),Integer.parseInt(cap.getText()),dir.getText().trim()));cargar.run();});});return p;
    }
    JPanel panelInventario(){
        JPanel p=basePanel();DefaultTableModel m=modelo("Código producto","Producto","Bodega","Cantidad disponible");p.add(new JScrollPane(tabla(m)),BorderLayout.CENTER);JButton a=boton("Agregar stock"),r=boton("Actualizar lista");JPanel s=new JPanel();s.setBackground(new Color(16,17,26));s.add(a);s.add(r);p.add(s,BorderLayout.SOUTH);Runnable cargar=()->{m.setRowCount(0);for(Existencia x:sistema.inventario){Producto pr=sistema.producto(x.productoCodigo);m.addRow(new Object[]{x.productoCodigo,pr==null?"":pr.nombre,x.bodegaId,x.cantidad});}};cargar.run();r.addActionListener(e->cargar.run());a.addActionListener(e->{JTextField cod=new JTextField(),bod=new JTextField(),can=new JTextField();Object[] f={"Código de producto:",cod,"ID de bodega:",bod,"Cantidad a agregar:",can};if(JOptionPane.showConfirmDialog(this,f,"Agregar al inventario",JOptionPane.OK_CANCEL_OPTION)==0)mensaje(()->{sistema.registrarExistencia(new Existencia(cod.getText().trim(),bod.getText().trim(),Integer.parseInt(can.getText())));cargar.run();});});return p;
    }
    JPanel panelOrdenes(){
        JPanel p=basePanel();DefaultTableModel m=modelo("Código orden","DPI cliente","Fecha","Total (Q)","Estado","Productos");p.add(new JScrollPane(tabla(m)),BorderLayout.CENTER);JButton crear=boton("Crear orden"),completar=boton("Completar venta"),cancelar=boton("Cancelar orden"),actualizar=boton("Actualizar lista");JPanel s=new JPanel();s.setBackground(new Color(16,17,26));s.add(crear);s.add(completar);s.add(cancelar);s.add(actualizar);p.add(s,BorderLayout.SOUTH);
        Runnable cargar=()->{m.setRowCount(0);for(OrdenCompra o:sistema.ordenes){StringBuilder det=new StringBuilder();for(LineaOrden l:o.lineas)det.append(l.productoCodigo).append(" x").append(l.cantidad).append("; ");m.addRow(new Object[]{o.codigo,o.clienteDpi,o.fecha,String.format(Locale.US,"%.2f",o.total),o.estado,det.toString()});}};cargar.run();actualizar.addActionListener(e->cargar.run());
        crear.addActionListener(e->{JTextField cod=new JTextField(),dpi=new JTextField();DefaultTableModel lm=modelo("Código producto","Cantidad");JTable lt=tabla(lm);JButton add=boton("Agregar producto a orden");JPanel box=new JPanel(new BorderLayout());box.add(new JScrollPane(lt),BorderLayout.CENTER);box.add(add,BorderLayout.SOUTH);add.addActionListener(ev->{JTextField pc=new JTextField(),ca=new JTextField();Object[] ff={"Código producto:",pc,"Cantidad:",ca};if(JOptionPane.showConfirmDialog(this,ff,"Detalle de orden",JOptionPane.OK_CANCEL_OPTION)==0)mensaje(()->{if(sistema.producto(pc.getText().trim())==null)throw new IllegalArgumentException("Producto no registrado.");int cant=Integer.parseInt(ca.getText());if(cant<=0)throw new IllegalArgumentException("Cantidad inválida.");lm.addRow(new Object[]{pc.getText().trim(),cant});});});Object[] f={"Código de orden:",cod,"DPI del cliente:",dpi,"Agregue al menos un producto:",box};if(JOptionPane.showConfirmDialog(this,f,"Crear orden de compra",JOptionPane.OK_CANCEL_OPTION)==0)mensaje(()->{ArrayList<LineaOrden> lineas=new ArrayList<>();for(int i=0;i<lm.getRowCount();i++)lineas.add(new LineaOrden(lm.getValueAt(i,0).toString(),Integer.parseInt(lm.getValueAt(i,1).toString())));OrdenCompra o=sistema.crearOrden(cod.getText().trim(),dpi.getText().trim(),lineas);cargar.run();JOptionPane.showMessageDialog(this,"Orden registrada. Total: Q "+String.format(Locale.US,"%.2f",o.total)+". Estado: Pendiente.");});});
        completar.addActionListener(e->{String cod=JOptionPane.showInputDialog(this,"Código de la orden que desea completar:");if(cod!=null&&!cod.trim().isEmpty())mensaje(()->{sistema.completarOrden(cod.trim());cargar.run();JOptionPane.showMessageDialog(this,"Venta completada y stock actualizado correctamente.");});});
        cancelar.addActionListener(e->{String cod=JOptionPane.showInputDialog(this,"Código de la orden que desea cancelar:");if(cod!=null&&!cod.trim().isEmpty())mensaje(()->{sistema.cancelarOrden(cod.trim());cargar.run();JOptionPane.showMessageDialog(this,"Orden cancelada.");});});return p;
    }
}

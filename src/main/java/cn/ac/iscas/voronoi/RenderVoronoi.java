package cn.ac.iscas.voronoi;

import de.alsclo.voronoi.Voronoi;
import de.alsclo.voronoi.graph.Point;

import javax.swing.*;
import java.awt.*;
import java.util.Random;
import java.util.stream.Collectors;
import java.util.stream.Stream;

public class RenderVoronoi extends JFrame {

    private static final int size = 512;

    private static final double POINT_SIZE = 5.0;
    private final Voronoi diagram;

    public RenderVoronoi(Voronoi diagram) {
        this.diagram = diagram;
    }

    public void paint(Graphics g) {
        Graphics2D g2 = (Graphics2D) g;
        for (Point site : diagram.getGraph().getSitePoints()) {
            g2.fillOval((int) Math.round(site.x - POINT_SIZE / 2),
                    size - (int) Math.round(site.y + POINT_SIZE / 2) + 32, (int) POINT_SIZE, (int) POINT_SIZE);
            //            g2.drawString(String.format("%d,%d", (int)site.x, (int)site.y), (int) site.x, size - (int)site.y + 32);
        }

        diagram.getGraph().edgeStream().filter(e -> e.getA() != null && e.getB() != null).forEach(e -> {
            Point a = e.getA().getLocation();
            Point b = e.getB().getLocation();
            g2.drawLine((int) a.x, size - (int) a.y + 32, (int) b.x, size - (int) b.y + 32);
        });
    }

    public static void main(String[] args) {
        Random r = new Random(9235563856L);
        Stream<Point> gen = Stream.generate(() -> new Point(r.nextDouble() * size, r.nextDouble() * size));
        Voronoi diagram = new Voronoi(gen.limit(1024).collect(Collectors.toList())).relax().relax();
        //        assert diagram.getGraph().edgeStream().noneMatch(e -> e.getA() == null && e.getB() == null);

        diagram.getGraph().edgeStream().forEach(System.out::println);

        // Point p1 = new Point(0.5, 1.5);
        // Point p2 = new Point(1.5, 1.5);
        // Point p3 = new Point(1.5, 0.5);
        // Point p4 = new Point(0.5, 0.5);

        // Point c = new Point(1, 1);

        // List<Point> pointDatas = Arrays.asList(c, p1, p2, p3, p4);
        // Collection<Point> points = pointDatas;
        // Voronoi diagram = new Voronoi(points);

        RenderVoronoi frame = new RenderVoronoi(diagram);
        frame.setDefaultCloseOperation(DISPOSE_ON_CLOSE);
        frame.setSize(size, size + 32);
        frame.setVisible(true);
    }
}
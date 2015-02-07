import java.applet.Applet;
import java.awt.*;
import java.util.*;
import java.util.List;

/**
 * Description
 * User: Drew
 * Version: 0.00
 */
public class Maze extends Applet {
    public static final int WTOP = 1;
    public static final int WRGT = 2;
    public static final int WBOT = 4;
    public static final int WLFT = 8;

    public boolean doBreak = true;
    private boolean isBroken = false;
    private boolean solved = false;

    private Map<Point, List<Point>> map = new HashMap<>();

    int cells[][];
    Point current_cell, start, end;
    Vector inlist;
    Vector outlist;
    Vector frontlist;
    int gridw, gridh, cellsize;
    Image offscreen;
    Graphics offgr;
    Thread t;

    int[][] weight;
    int[][] dist;
    boolean[][] visited;

    boolean mazeDone = false;
    Point current = null;
    long startTime;

    Map<Point, Point> parentMap = new HashMap<>();

    LinkedList<Point> path = new LinkedList<>();

    public void init()
    {
        int x, y;
        String param;



        gridw = 20;
        gridh = 20;
        cellsize = 40;

        cells = new int[gridw][gridh];
        weight = new int[gridw][gridh];
        dist = new int[gridw][gridh];
        visited = new boolean[gridw][gridh];
        /* Or together the wall bits to show that all
           * the walls are up.
           */
        int full = WTOP | WBOT | WLFT | WRGT;
        for (x = 0; x < gridw; x++)
            for (y = 0; y < gridh; y++) {
                cells[x][y] = full;
                weight[x][y] = rnd(100);
            }
        /* Then, mark the borders
           */
        int left = WLFT << 4;
        int right = WRGT << 4;
        for (y = 0; y < gridh; y++)
        {
            cells[0][y] |= left;
            cells[gridw-1][y] |= right;
        }
        int top = WTOP << 4;
        int bottom = WBOT << 4;
        for (x = 0; x < gridw; x++)
        {
            cells[x][0] |= top;
            cells[x][gridh-1] |= bottom;
        }
        offscreen = createImage((gridw+2)*cellsize, (gridh+2)*cellsize);
        offgr = offscreen.getGraphics();

        t = new Thread(new Runnable() {
            @Override
            public void run() {
                while(!solved) {
                    if(!mazeDone) {
                        createMaze();
                    } else if(doBreak && !isBroken) {
                        for(int i = 0; i < rnd(5); i++) {
                            Point p = new Point(rnd(gridw)-1,rnd(gridh)-1);
                            System.out.println("Removing wall from " + p);
                            removeWall(p, findInNbr(p));
                        }
                        isBroken = true;
                    } else {
                        startTime = System.currentTimeMillis();
                        solveMaze();
                        solved = true;
                    }
                }
                System.out.println("Completed in " + (System.currentTimeMillis() - startTime) + " milliseconds.");
                return;
            }
        });
        t.start();
    }

    public void solveMaze() {
        Set<Point> unvisited = new HashSet<>();
        for(int i = 0; i < cells.length; i++) {
            for(int j = 0; j < cells[i].length; j++) {
                dist[i][j] = Integer.MAX_VALUE;
                visited[i][j] = false;
                if(i == 0 && j == 0) continue;

                unvisited.add(new Point(i, j));
            }
        }
        dist[start.x][start.y] = 0;
        current = new Point(start.x, start.y);

        while(!unvisited.isEmpty()) {
            for(Point p : getAdjacentPoints(current)) {
                if(visited[p.x][p.y]) continue;
                int newVal = dist[current.x][current.y] + weight[p.x][p.y];
                if(newVal < dist[p.x][p.y]) {
                    dist[p.x][p.y] = newVal;
                    parentMap.put(p, current);
                }
            }
            visited[current.x][current.y] = true;
            unvisited.remove(current);
            if(visited[end.x][end.y]) {
                break;
            }
            int min = Integer.MAX_VALUE;
            for(Point p : unvisited) {
                if(dist[p.x][p.y] < min) {
                    min = dist[p.x][p.y];
                    current = p;
                }
            }
        }
        Point curr = end;
        while(curr != null && !curr.equals(start)) {
            path.add(curr);
            curr = parentMap.get(curr);
        }
        path.add(start);
        Collections.reverse(path);
        repaint();
    }

    public void createMaze()
    {
        int dir, x, y;

        /*
           * Implement Prim's algorithm to build maze
           */
        outlist = new Vector(gridw*gridh);
        inlist = new Vector(10,10);
        frontlist = new Vector(10,10);
        for (x = 0; x < gridw; x++)
            for (y = 0; y < gridh; y++)
                outlist.addElement(new Point(x,y));

        start = new Point(0, 0);
        end = new Point(rnd(gridw), gridh-1);

        current_cell = (Point)rndElement(outlist);
        inlist.addElement(current_cell);
        moveNbrs(current_cell);

        while (!frontlist.isEmpty())
        {
            current_cell = (Point)rndElement(frontlist);
            inlist.addElement(current_cell);
            moveNbrs(current_cell);
            dir = findInNbr(current_cell);
            removeWall(current_cell, dir);
            /*
                * Break for the animation effect
                */
            repaint();
            try
            {
                Thread.sleep(30);
            } catch (Exception e) {};
        }
        /*
           * All done
           */
        current_cell = null;
        mazeDone = true;
        repaint();
    }

    public void update(Graphics g)
    {
        paint(offgr);
        g.drawImage(offscreen,0,0,this);
    }

    //   int show = 1;

    public void paint(Graphics g)
    {
        int val, x ,y;

        int basex = 10;
        int basey = 10;
        g.setColor(Color.BLACK);
        g.fillRect(basex, basey, gridw*cellsize, gridh*cellsize);
        g.setColor(Color.MAGENTA);
        if(start != null && end != null) {
            g.fillRect(basex+start.x*cellsize + 1, basey+start.y*cellsize +1, cellsize -2, cellsize - 2);
            g.fillRect(basex+end.x*cellsize + 1, basey+end.y*cellsize +1, cellsize -2, cellsize - 2);
        }

        if(path.size() > 0) {
            g.setColor(Color.RED);
            for(Point p : path) {
                if(p.equals(end) || p.equals(start)) continue;
                g.fillRect(basex+p.x*cellsize + 1, basey+p.y*cellsize+1, cellsize-2, cellsize-2);
            }
        }
        g.setColor(Color.GRAY);
        for (x = 0; x < gridw; x++)
            for (y = 0; y < gridh; y++)
            {
                if(cellsize >= 30) {
                    g.drawString("" + weight[x][y], (int)(1.5*basex) + x*cellsize, 3*basey+y*cellsize);
                }
                val = cells[x][y];
                Graphics2D g2 = ((Graphics2D)g);
                g2.setStroke(new BasicStroke(1));
                if ((val & WTOP) != 0)
                    g2.drawLine(basex+x*cellsize, basey+y*cellsize,
                            basex+(x+1)*cellsize, basey+y*cellsize);
                if ((val & WRGT) != 0)
                    g2.drawLine(basex+(x+1)*cellsize-1, basey+y*cellsize,
                            basex+(x+1)*cellsize-1, basey+(y+1)*cellsize);
                if ((val & WBOT) != 0)
                    g2.drawLine(basex+x*cellsize, basey+(y+1)*cellsize-1,
                            basex+(x+1)*cellsize, basey+(y+1)*cellsize-1);
                if ((val & WLFT) != 0)
                    g2.drawLine(basex + x * cellsize, basey + y * cellsize,
                            basex + x * cellsize, basey + (y + 1) * cellsize);
            }

        /*
           * Draw the current_cell as well
           */
        g.setColor(Color.WHITE);
        if (current_cell != null)
            g.fillOval(basex+current_cell.x*cellsize, basey+current_cell.y*cellsize,
                    cellsize, cellsize);
    }

    /*
      * The following routines provide access to the underlying
      * maze data structure.
      */
    int findInNbr(Point p)
    {
        /* Return a random direction in which the point p has
           * a neighbor which is in inlist.
           */
        int d = rnd(4)-1;
        int k = 0;
        while (k < 4)
        {
            switch(d)
            {
                case 0:	/* Top nbr? */
                    if ((cells[p.x][p.y] & (WTOP<<4)) != 0) break;
                    if (inlist.indexOf(new Point(p.x,p.y-1)) >= 0)
                        return WTOP;
                    break;
                case 1: /* Right nbr? */
                    if ((cells[p.x][p.y] & (WRGT<<4)) != 0) break;
                    if (inlist.indexOf(new Point(p.x+1,p.y)) >= 0)
                        return WRGT;
                    break;
                case 2: /* Bottom nbr? */
                    if ((cells[p.x][p.y] & (WBOT<<4)) != 0) break;
                    if (inlist.indexOf(new Point(p.x,p.y+1)) >= 0)
                        return WBOT;
                    break;
                case 3: /* Left nbr? */
                    if ((cells[p.x][p.y] & (WLFT<<4)) != 0) break;
                    if (inlist.indexOf(new Point(p.x-1,p.y)) >= 0)
                        return WLFT;
                    break;
            }
            d = (d+1) % 4;
            k++;
        }
        return 0; // This shouldn't ever happen
    }

    void moveNbrs(Point p)
    {
        Point s;

        /*
           * Move any neighbors of p which are in outlist from
           * outlist to frontlist.
           */
        if ((cells[p.x][p.y] & (WTOP<<4)) == 0)
        {
            s = new Point(p.x, p.y-1);
            movePoint(s, outlist, frontlist);
        }
        if ((cells[p.x][p.y] & (WRGT<<4)) == 0)
        {
            s = new Point(p.x+1, p.y);
            movePoint(s, outlist, frontlist);
        }
        if ((cells[p.x][p.y] & (WBOT<<4)) == 0)
        {
            s = new Point(p.x, p.y+1);
            movePoint(s, outlist, frontlist);
        }
        if ((cells[p.x][p.y] & (WLFT<<4)) == 0)
        {
            s = new Point(p.x-1, p.y);
            movePoint(s, outlist, frontlist);
        }
    }

    Point[] getAdjacentPoints(Point p) {
        int cell = cells[p.x][p.y];
        int val = cells[p.x][p.y];
        ArrayList<Point> adjacent = new ArrayList<>();

        if ((val & WTOP) == 0)
            adjacent.add(new Point(p.x, p.y - 1));
        if ((val & WRGT) == 0)
            adjacent.add(new Point(p.x+1, p.y));
        if ((val & WBOT) == 0)
            adjacent.add(new Point(p.x, p.y+1));
        if ((val & WLFT) == 0)
            adjacent.add(new Point(p.x-1, p.y));
        Point[] points = new Point[adjacent.size()];
        return adjacent.toArray(points);
    }

    void movePoint(Point p, Vector v, Vector w)
    {
        /*
           * If p is element of v, move it to w
           */
        int i = v.indexOf(p);
        if (i >= 0)
        {
            v.removeElementAt(i);
            w.addElement(p);
        }
    }

    void removeWall(Point p, int d)
    {
        /* Exclusive or bit with cell to drop wall
           */
        cells[p.x][p.y] ^= d;
        /*
           * And drop neighboring wall as well
           */
        switch(d)
        {
            case WTOP: cells[p.x][p.y-1] ^= WBOT;
                break;
            case WRGT: cells[p.x+1][p.y] ^= WLFT;
                break;
            case WBOT: cells[p.x][p.y+1] ^= WTOP;
                break;
            case WLFT: cells[p.x-1][p.y] ^= WRGT;
                break;
        }
    }

    // Utility routines
    int rnd(int n)
    {
        return (int)(Math.random()*n+1);
    }

    Object rndElement(Vector v)
    {
        int i = rnd(v.size())-1;
        Object s = v.elementAt(i);
        v.removeElementAt(i);
        return s;
    }
}
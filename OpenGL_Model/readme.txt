Added design features:
implemented: "Add exhaust pipes"
inside directory "Car" -> class Back.java -> render :
We translated to the bottom back of the car relative to it's coordinate system, then rotated in 90 degrees to get the angle we wanted.
then we rendered 2 gluCylinder next to each other as the exhaust pipes.

(Bonus) implemented: Any other decorations that you see fit (using our imagination)
inside directory "Car" -> class Center.java -> render :
Cars mirrors: we translated to the corners of the center part and added 2 gluCylinder in each corner that holds the mirrors, the mirrors are skewed boxes rotated on the side to the driver's comfort.
/*  instances of this class represent
    input events

    the class provides a single queue
    for input events
*/

import java.util.ArrayList;

public class InputInfo
{
  //------------------- the input info queue -----------------------------
  // inefficiency of the queue is unimportant
  // so keep it simple
  private static ArrayList<InputInfo> queue = new ArrayList<InputInfo>();

  public static void add( InputInfo info )
  {
    queue.add( info );
  }

  public static int size()
  {
    return queue.size();
  }

  public static InputInfo get()
  {
    InputInfo info = queue.get( 0 );
    queue.remove( 0 );
    return info;
  }

  //------------------- individual InputInfo -----------------------------

  public char kind;  // the kind of input event
  public int code;  // the particular key or button that was used like GLFW_KEY_A is 65
  public int action;  // press, repeat, release
  public int mods;  // 1=shift, 2=control, 4=alt/option, can do several
  public int mouseX, mouseY;  // mouse cursor position

  public String toString()
  {
    if( kind == 'k' )
      return "[key #: " + code + " action: " + action +
             " mods: " + mods + "]";
    else if( kind == 'm' )
      return "[mouse moved to " + mouseX + " " + mouseY + "]";
    else if( kind == 'b' )
      return "[mouse button " + code + " action: " + action + " mods: " + mods + "]";
    else
      return "unknown kind of InputInfo";
  }

  // construct info for key or mouse press,release,repeat
  public InputInfo( char knd, int source, int act, int shifts )
  {
    kind = knd;
    code = source;
    action = act;
    mods = shifts;
  }

  // construct info for mouse move
  public InputInfo( char knd, int x, int y )
  {
    kind = knd;
    mouseX = x;  mouseY = y;
  }

}

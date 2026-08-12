# Spring endpoints (Legacy fms2026)

| Controller | Class mapping | Method mapping | Method |
|---|---|---|---|
| `com-amygo/com-amygo-fms/src/main/java/com/amygo/fms/interfaces/web/AdministratorController.java` | "/administrator" | "/administrator") public class AdministratorController {      @Autowired     protected AdministratorService administrato | `create` |
| `com-amygo/com-amygo-fms/src/main/java/com/amygo/fms/interfaces/web/AdministratorController.java` | "/administrator" | method = RequestMethod.GET | `list` |
| `com-amygo/com-amygo-fms/src/main/java/com/amygo/fms/interfaces/web/AdministratorController.java` | "/administrator" | value = "/logout", method = RequestMethod.GET | `logout` |
| `com-amygo/com-amygo-fms/src/main/java/com/amygo/fms/interfaces/web/AdministratorController.java` | "/administrator" | value = "/modify-profile", method = RequestMethod.POST | `modifyProfile` |
| `com-amygo/com-amygo-fms/src/main/java/com/amygo/fms/interfaces/web/AdministratorController.java` | "/administrator" | value = "/profile", method = RequestMethod.GET | `myinfo` |
| `com-amygo/com-amygo-fms/src/main/java/com/amygo/fms/interfaces/web/AdministratorController.java` | "/administrator" | value = "/{id}/grant-role",method = RequestMethod.POST | `grantRole` |
| `com-amygo/com-amygo-fms/src/main/java/com/amygo/fms/interfaces/web/AdministratorController.java` | "/administrator" | value = "/{id}/modify", method = RequestMethod.POST | `modify` |
| `com-amygo/com-amygo-fms/src/main/java/com/amygo/fms/interfaces/web/AdministratorController.java` | "/administrator" | value = "/{id}/select-role", method = RequestMethod.GET | `selectRole` |
| `com-amygo/com-amygo-fms/src/main/java/com/amygo/fms/interfaces/web/AdministratorController.java` | "/administrator" | value = "/{id}/status",method = RequestMethod.PUT)     @ResponseBody     public void switchStatus(@PathVariable("id") St | `form` |
| `com-amygo/com-amygo-fms/src/main/java/com/amygo/fms/interfaces/web/CarController.java` | "/car" | "/car") public class CarController {      @Autowired     protected CarService carService;     @Autowired     protected C | `create` |
| `com-amygo/com-amygo-fms/src/main/java/com/amygo/fms/interfaces/web/CarController.java` | "/car" | method = RequestMethod.GET | `list` |
| `com-amygo/com-amygo-fms/src/main/java/com/amygo/fms/interfaces/web/CarController.java` | "/car" | value = "/carMonitor",method = RequestMethod.GET | `getCars` |
| `com-amygo/com-amygo-fms/src/main/java/com/amygo/fms/interfaces/web/CarController.java` | "/car" | value = "/{id}/modify", method = RequestMethod.POST | `modify` |
| `com-amygo/com-amygo-fms/src/main/java/com/amygo/fms/interfaces/web/CarController.java` | "/car" | value = "/{id}/status", method = RequestMethod.PUT)     @ResponseBody     public void switchStatus(@PathVariable("id") S | `toform` |
| `com-amygo/com-amygo-fms/src/main/java/com/amygo/fms/interfaces/web/CarController.java` | "/car" | value = "/{id}/view", method = RequestMethod.GET | `view` |
| `com-amygo/com-amygo-fms/src/main/java/com/amygo/fms/interfaces/web/CarPathPlanController.java` | "/carPathPlan" | "/carPathPlan") public class CarPathPlanController {      @Autowired     protected CarService carService;     @Autowired | `list` |
| `com-amygo/com-amygo-fms/src/main/java/com/amygo/fms/interfaces/web/CarRemoteControlController.java` | "/carRemoteControl" | "/carRemoteControl") public class CarRemoteControlController {      @Autowired     protected CarService carService;      | `list` |
| `com-amygo/com-amygo-fms/src/main/java/com/amygo/fms/interfaces/web/CarStatusController.java` | "/carStatus" | "/carStatus") public class CarStatusController {  	@Autowired 	protected CarStatusService carStatusService;   	@RequestM | `toform` |
| `com-amygo/com-amygo-fms/src/main/java/com/amygo/fms/interfaces/web/CarStatusController.java` | "/carStatus" | method = RequestMethod.GET | `list` |
| `com-amygo/com-amygo-fms/src/main/java/com/amygo/fms/interfaces/web/CarStatusRecordController.java` | "/carStatusRecord" | "/carStatusRecord") public class CarStatusRecordController {  	@Autowired 	protected CarStatusRecordService carStatusRec | `view` |
| `com-amygo/com-amygo-fms/src/main/java/com/amygo/fms/interfaces/web/EvaluationController.java` | "/evaluation" | "/evaluation") public class EvaluationController { 	@Autowired 	private EvaluationService evaluationService; 	@Autowired | `replyUI` |
| `com-amygo/com-amygo-fms/src/main/java/com/amygo/fms/interfaces/web/EvaluationController.java` | "/evaluation" | value = "/reply", method = RequestMethod.POST | `reply` |
| `com-amygo/com-amygo-fms/src/main/java/com/amygo/fms/interfaces/web/EvaluationController.java` | "/evaluation" | value = "/{id}/viewUI", method = RequestMethod.GET | `viewUI` |
| `com-amygo/com-amygo-fms/src/main/java/com/amygo/fms/interfaces/web/FeedbackController.java` | "/feedback" | "/feedback") public class FeedbackController { 	@Autowired 	private FeedbackService feedbackService; 	@Autowired 	privat | `replyUI` |
| `com-amygo/com-amygo-fms/src/main/java/com/amygo/fms/interfaces/web/FeedbackController.java` | "/feedback" | value = "/reply", method = RequestMethod.POST | `reply` |
| `com-amygo/com-amygo-fms/src/main/java/com/amygo/fms/interfaces/web/FeedbackController.java` | "/feedback" | value = "/{id}/viewUI", method = RequestMethod.GET | `viewUI` |
| `com-amygo/com-amygo-fms/src/main/java/com/amygo/fms/interfaces/web/IndexController.java` |  | "/" | `index` |
| `com-amygo/com-amygo-fms/src/main/java/com/amygo/fms/interfaces/web/MenuController.java` | "/menu" | "/menu") public class MenuController {      @Autowired     protected MenuService menuService;      @RequestMapping(metho | `create` |
| `com-amygo/com-amygo-fms/src/main/java/com/amygo/fms/interfaces/web/MenuController.java` | "/menu" | method = RequestMethod.GET | `list` |
| `com-amygo/com-amygo-fms/src/main/java/com/amygo/fms/interfaces/web/MenuController.java` | "/menu" | value = "/{id}/modify", method = RequestMethod.POST | `modify` |
| `com-amygo/com-amygo-fms/src/main/java/com/amygo/fms/interfaces/web/MenuController.java` | "/menu" | value = "/{id}/status", method = RequestMethod.PUT)     @ResponseBody     public void switchStatus(@PathVariable("id") S | `toform` |
| `com-amygo/com-amygo-fms/src/main/java/com/amygo/fms/interfaces/web/OrdersController.java` | "/orders" | "/orders") public class OrdersController {      @Autowired     protected OrdersService ordersService;      @RequestMappi | `view` |
| `com-amygo/com-amygo-fms/src/main/java/com/amygo/fms/interfaces/web/OrdersController.java` | "/orders" | method = RequestMethod.GET | `list` |
| `com-amygo/com-amygo-fms/src/main/java/com/amygo/fms/interfaces/web/ResourceController.java` | "/resource" | "/resource") public class ResourceController {      @Autowired     protected ResourceService resourceService;      @Requ | `create` |
| `com-amygo/com-amygo-fms/src/main/java/com/amygo/fms/interfaces/web/ResourceController.java` | "/resource" | method = RequestMethod.GET | `list` |
| `com-amygo/com-amygo-fms/src/main/java/com/amygo/fms/interfaces/web/ResourceController.java` | "/resource" | value = "/{id}/modify", method = RequestMethod.POST | `modify` |
| `com-amygo/com-amygo-fms/src/main/java/com/amygo/fms/interfaces/web/ResourceController.java` | "/resource" | value = "/{id}/status",method = RequestMethod.PUT)     @ResponseBody     public void switchStatus(@PathVariable("id") St | `toform` |
| `com-amygo/com-amygo-fms/src/main/java/com/amygo/fms/interfaces/web/RoleController.java` | "/role" | "/role") public class RoleController {      @Autowired     protected RoleService roleService;      @RequestMapping(metho | `create` |
| `com-amygo/com-amygo-fms/src/main/java/com/amygo/fms/interfaces/web/RoleController.java` | "/role" | method = RequestMethod.GET | `list` |
| `com-amygo/com-amygo-fms/src/main/java/com/amygo/fms/interfaces/web/RoleController.java` | "/role" | value = "/{id}/grant-menu",method = RequestMethod.POST | `grantMenu` |
| `com-amygo/com-amygo-fms/src/main/java/com/amygo/fms/interfaces/web/RoleController.java` | "/role" | value = "/{id}/grant-resource",method = RequestMethod.POST | `grantResources` |
| `com-amygo/com-amygo-fms/src/main/java/com/amygo/fms/interfaces/web/RoleController.java` | "/role" | value = "/{id}/modify", method = RequestMethod.POST | `modify` |
| `com-amygo/com-amygo-fms/src/main/java/com/amygo/fms/interfaces/web/RoleController.java` | "/role" | value = "/{id}/select-menu", method = RequestMethod.GET | `selectMenu` |
| `com-amygo/com-amygo-fms/src/main/java/com/amygo/fms/interfaces/web/RoleController.java` | "/role" | value = "/{id}/select-resource", method = RequestMethod.GET | `selectRole` |
| `com-amygo/com-amygo-fms/src/main/java/com/amygo/fms/interfaces/web/RoleController.java` | "/role" | value = "/{id}/status",method = RequestMethod.PUT)     @ResponseBody     public void switchStatus(@PathVariable("id") St | `toform` |
| `com-amygo/com-amygo-fms/src/main/java/com/amygo/fms/interfaces/web/StatisticsController.java` | "/statistics" | "/statistics") public class StatisticsController {      @Autowired     protected CarService carService;     @Autowired   | `list` |
| `com-amygo/com-amygo-fms/src/main/java/com/amygo/fms/interfaces/web/SysLogController.java` | "/syslog" | "/syslog") public class SysLogController {      @Autowired     protected SysLogService sysLogService;            @Reques | `list` |
| `com-amygo/com-amygo-fms/src/main/java/com/amygo/fms/interfaces/web/SystemVariableController.java` | "/systemVariable" | "/systemVariable") public class SystemVariableController {  	@Autowired 	protected SystemVariableService systemVariableS | `create` |
| `com-amygo/com-amygo-fms/src/main/java/com/amygo/fms/interfaces/web/SystemVariableController.java` | "/systemVariable" | value = "/addOperationRegion", method = RequestMethod.GET, produces = "application/json") 	@ResponseBody 	public BaseRes | `operationTimeList` |
| `com-amygo/com-amygo-fms/src/main/java/com/amygo/fms/interfaces/web/SystemVariableController.java` | "/systemVariable" | value = "/addOperationRegionUI", method = RequestMethod.GET | `addOperationRegionUI` |
| `com-amygo/com-amygo-fms/src/main/java/com/amygo/fms/interfaces/web/SystemVariableController.java` | "/systemVariable" | value = "/electronicFence", method = RequestMethod.GET | `electronicFence` |
| `com-amygo/com-amygo-fms/src/main/java/com/amygo/fms/interfaces/web/SystemVariableController.java` | "/systemVariable" | value = "/operationRegionList", method = RequestMethod.GET | `operationRegionList` |
| `com-amygo/com-amygo-fms/src/main/java/com/amygo/fms/interfaces/web/SystemVariableController.java` | "/systemVariable" | value = "/{id}/delete", method = RequestMethod.DELETE) 	@ResponseBody 	public void delete(@PathVariable("id") Long id) { | `toform` |
| `com-amygo/com-amygo-fms/src/main/java/com/amygo/fms/interfaces/web/SystemVariableController.java` | "/systemVariable" | value = "/{id}/modify", method = RequestMethod.POST | `modify` |
| `com-amygo/com-amygo-fms/src/main/java/com/amygo/fms/interfaces/web/SystemVariableController.java` | "/systemVariable" | value = "/{id}/updateOperationRegionUI", method = RequestMethod.GET | `updateOperationRegionUI` |
| `com-amygo/com-amygo-fms/src/main/java/com/amygo/fms/interfaces/web/SystemVariableController.java` | "/systemVariable" | value = "/{id}/view", method = RequestMethod.GET | `list` |
| `com-amygo/com-amygo-fms/src/main/java/com/amygo/fms/interfaces/web/UserController.java` | "/user" | "/user") public class UserController {      @Autowired     protected UserService userService;      @RequestMapping(value | `view` |
| `com-amygo/com-amygo-fms/src/main/java/com/amygo/fms/interfaces/web/UserController.java` | "/user" | method = RequestMethod.GET | `list` |
| `com-website/com-website-cms/src/main/java/com/website/cms/interfaces/web/AdministratorController.java` | "/administrator" | "/administrator") public class AdministratorController {      @Autowired     protected AdministratorService administrato | `create` |
| `com-website/com-website-cms/src/main/java/com/website/cms/interfaces/web/AdministratorController.java` | "/administrator" | method = RequestMethod.GET | `list` |
| `com-website/com-website-cms/src/main/java/com/website/cms/interfaces/web/AdministratorController.java` | "/administrator" | value = "/logout", method = RequestMethod.GET | `logout` |
| `com-website/com-website-cms/src/main/java/com/website/cms/interfaces/web/AdministratorController.java` | "/administrator" | value = "/modify-profile", method = RequestMethod.POST | `modifyProfile` |
| `com-website/com-website-cms/src/main/java/com/website/cms/interfaces/web/AdministratorController.java` | "/administrator" | value = "/profile", method = RequestMethod.GET | `myinfo` |
| `com-website/com-website-cms/src/main/java/com/website/cms/interfaces/web/AdministratorController.java` | "/administrator" | value = "/{id}/grant-role",method = RequestMethod.POST | `grantRole` |
| `com-website/com-website-cms/src/main/java/com/website/cms/interfaces/web/AdministratorController.java` | "/administrator" | value = "/{id}/modify", method = RequestMethod.POST | `modify` |
| `com-website/com-website-cms/src/main/java/com/website/cms/interfaces/web/AdministratorController.java` | "/administrator" | value = "/{id}/select-role", method = RequestMethod.GET | `selectRole` |
| `com-website/com-website-cms/src/main/java/com/website/cms/interfaces/web/AdministratorController.java` | "/administrator" | value = "/{id}/status",method = RequestMethod.PUT)     @ResponseBody     public void switchStatus(@PathVariable("id") St | `form` |
| `com-website/com-website-cms/src/main/java/com/website/cms/interfaces/web/FileController.java` | "/file" | "/file") public class FileController {      @Autowired     protected ImageService imageService;          @Autowired      | `create` |
| `com-website/com-website-cms/src/main/java/com/website/cms/interfaces/web/FileController.java` | "/file" | method = RequestMethod.GET | `list` |
| `com-website/com-website-cms/src/main/java/com/website/cms/interfaces/web/FileController.java` | "/file" | value = "/{id}/delete", method = RequestMethod.DELETE)     @ResponseBody     public void delete(@PathVariable("id") Long | `toform` |
| `com-website/com-website-cms/src/main/java/com/website/cms/interfaces/web/FileController.java` | "/file" | value = "/{id}/modify", method = RequestMethod.POST | `modify` |
| `com-website/com-website-cms/src/main/java/com/website/cms/interfaces/web/FileController.java` | "/file" | value = "/{id}/view", method = RequestMethod.GET | `view` |
| `com-website/com-website-cms/src/main/java/com/website/cms/interfaces/web/ImageController.java` | "/image" | "/image") public class ImageController {      @Autowired     protected ImageService imageService;          @Autowired    | `create` |
| `com-website/com-website-cms/src/main/java/com/website/cms/interfaces/web/ImageController.java` | "/image" | method = RequestMethod.GET | `list` |
| `com-website/com-website-cms/src/main/java/com/website/cms/interfaces/web/ImageController.java` | "/image" | value = "/{id}/delete", method = RequestMethod.DELETE)     @ResponseBody     public void delete(@PathVariable("id") Long | `toform` |
| `com-website/com-website-cms/src/main/java/com/website/cms/interfaces/web/ImageController.java` | "/image" | value = "/{id}/modify", method = RequestMethod.POST | `modify` |
| `com-website/com-website-cms/src/main/java/com/website/cms/interfaces/web/ImageController.java` | "/image" | value = "/{id}/view", method = RequestMethod.GET | `view` |
| `com-website/com-website-cms/src/main/java/com/website/cms/interfaces/web/IndexController.java` |  | "/" | `index` |
| `com-website/com-website-cms/src/main/java/com/website/cms/interfaces/web/MenuController.java` | "/menu" | "/menu") public class MenuController {      @Autowired     protected MenuService menuService;      @RequestMapping(metho | `create` |
| `com-website/com-website-cms/src/main/java/com/website/cms/interfaces/web/MenuController.java` | "/menu" | method = RequestMethod.GET | `list` |
| `com-website/com-website-cms/src/main/java/com/website/cms/interfaces/web/MenuController.java` | "/menu" | value = "/{id}/modify", method = RequestMethod.POST | `modify` |
| `com-website/com-website-cms/src/main/java/com/website/cms/interfaces/web/MenuController.java` | "/menu" | value = "/{id}/status", method = RequestMethod.PUT)     @ResponseBody     public void switchStatus(@PathVariable("id") S | `toform` |
| `com-website/com-website-cms/src/main/java/com/website/cms/interfaces/web/PageController.java` | "/page" | "/page") public class PageController {      @Autowired     protected ImageService imageService;          @Autowired      | `create` |
| `com-website/com-website-cms/src/main/java/com/website/cms/interfaces/web/PageController.java` | "/page" | method = RequestMethod.GET | `list` |
| `com-website/com-website-cms/src/main/java/com/website/cms/interfaces/web/PageController.java` | "/page" | value = "/{id}/delete", method = RequestMethod.DELETE)     @ResponseBody     public void delete(@PathVariable("id") Long | `toform` |
| `com-website/com-website-cms/src/main/java/com/website/cms/interfaces/web/PageController.java` | "/page" | value = "/{id}/modify", method = RequestMethod.POST | `modify` |
| `com-website/com-website-cms/src/main/java/com/website/cms/interfaces/web/PageController.java` | "/page" | value = "/{id}/view", method = RequestMethod.GET | `view` |
| `com-website/com-website-cms/src/main/java/com/website/cms/interfaces/web/ResourceController.java` | "/resource" | "/resource") public class ResourceController {      @Autowired     protected ResourceService resourceService;      @Requ | `create` |
| `com-website/com-website-cms/src/main/java/com/website/cms/interfaces/web/ResourceController.java` | "/resource" | method = RequestMethod.GET | `list` |
| `com-website/com-website-cms/src/main/java/com/website/cms/interfaces/web/ResourceController.java` | "/resource" | value = "/{id}/modify", method = RequestMethod.POST | `modify` |
| `com-website/com-website-cms/src/main/java/com/website/cms/interfaces/web/ResourceController.java` | "/resource" | value = "/{id}/status",method = RequestMethod.PUT)     @ResponseBody     public void switchStatus(@PathVariable("id") St | `toform` |
| `com-website/com-website-cms/src/main/java/com/website/cms/interfaces/web/RoleController.java` | "/role" | "/role") public class RoleController {      @Autowired     protected RoleService roleService;      @RequestMapping(metho | `create` |
| `com-website/com-website-cms/src/main/java/com/website/cms/interfaces/web/RoleController.java` | "/role" | method = RequestMethod.GET | `list` |
| `com-website/com-website-cms/src/main/java/com/website/cms/interfaces/web/RoleController.java` | "/role" | value = "/{id}/grant-menu",method = RequestMethod.POST | `grantMenu` |
| `com-website/com-website-cms/src/main/java/com/website/cms/interfaces/web/RoleController.java` | "/role" | value = "/{id}/grant-resource",method = RequestMethod.POST | `grantResources` |
| `com-website/com-website-cms/src/main/java/com/website/cms/interfaces/web/RoleController.java` | "/role" | value = "/{id}/modify", method = RequestMethod.POST | `modify` |
| `com-website/com-website-cms/src/main/java/com/website/cms/interfaces/web/RoleController.java` | "/role" | value = "/{id}/select-menu", method = RequestMethod.GET | `selectMenu` |
| `com-website/com-website-cms/src/main/java/com/website/cms/interfaces/web/RoleController.java` | "/role" | value = "/{id}/select-resource", method = RequestMethod.GET | `selectRole` |
| `com-website/com-website-cms/src/main/java/com/website/cms/interfaces/web/RoleController.java` | "/role" | value = "/{id}/status",method = RequestMethod.PUT)     @ResponseBody     public void switchStatus(@PathVariable("id") St | `toform` |
| `com-website/com-website-cms/src/main/java/com/website/cms/interfaces/web/SysLogController.java` | "/syslog" | "/syslog") public class SysLogController {      @Autowired     protected SysLogService sysLogService;            @Reques | `list` |

Total mapped methods: 100

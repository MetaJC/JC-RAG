export interface MenuNode {
  id: number
  parentId?: number | null
  code?: string
  title: string
  path?: string
  icon?: string
  sort?: number
  component?: string
  visible?: number
  status?: number
  children?: MenuNode[]
}

export interface AuthUser {
  userId: number
  username: string
  nickname: string
  userType: string
  /** 用户拥有的全部 permCode 集合（去重） */
  permCodes?: string[]
  /** 用户可见的菜单树（permType=MENU + status=1 + 角色绑定） */
  menus?: MenuNode[]
}

export interface LoginRequest {
  username: string
  password: string
  /** 滑动验证码 token (后端一次性消费) */
  captchaToken?: string
}

export interface ChangePasswordRequest {
  oldPassword: string
  newPassword: string
}
